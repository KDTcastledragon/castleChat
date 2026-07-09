< 메시지 Kafka 비동기 아키텍처 설계 문서 (SEND / DELETE / REACT_MESSAGE) >

doc.md의 prpr 준수 검증:
- prpr 1 (대용량 트래픽) : Kafka 파티션 분산 + consumer group 수평확장 + Snowflake 로컬 채번(중앙 병목 없음)으로 대응.
- prpr 2 (Client UX 우선) : client 응답은 DB 상태와 무관. DB가 느려져도 응답속도 영향 없음.
- prpr 3 (sendMessage) : DB insert 후 response 하지 않는다. kafka durable save(acks=all) 후 response 한다. DB insert는 kafka consumer가 비동기 처리.
- prpr 4 (readMessage) : 이번 작업에서 변경 없음. 기존 Redis + dirty flush worker 구조 유지.
- prpr 5 (naming) : 기존 코드 스타일(createChatMessage, flushDirtyReadPositions 등 camelCase + 도메인 접두 클래스명) 그대로 따름.

===========================================================================================

A. 전체 흐름

[기존 흐름 (변경 전)]
fe --ws--> websocket-gateway --gRPC--> cheg.ChatGrpcEndpoint
  -> ChatCommandService.createChatMessage()
       -> DB insert (동기) -> auto_increment로 messageId 발급
       -> response 조립 -> client 응답        <== prpr 3 위반 (DB insert 후 response)

[신규 흐름 (변경 후)]
fe --ws--> websocket-gateway --gRPC--> cheg.ChatGrpcEndpoint
  -> ChatCommandService.createChatMessage()
       1. 검증 (멤버십은 Redis RoomMemberCache 우선, miss시에만 DB select)
       2. messageId = ChatMessageIdGenerator.nextMessageId()   <== Snowflake. DB 안 감.
       3. (첨부 있으면) TEMP 첨부 row를 attachmentIds로 select (읽기만. 응답용)
       4. Kafka publish "castlechat.chat.message" (key=roomId)
          -> acks=all 브로커 기록 확인(durable save)까지 대기. 실패/타임아웃이면 client에 에러.
       5. Redis lrm updateIfGreater (sender 본인 읽음위치. 기존과 동일)
       6. response 조립 (DB insert 결과가 아니라 메모리의 값으로) -> client 응답
  ---- 여기서 client는 이미 응답 받음. 이후는 전부 비동기 ----
cheg.ChatMessagePersistWorker (@KafkaListener, consumer group: chengine-chat-persist)
       7. 이벤트 수신 -> chat_messages에 INSERT IGNORE (messageId 명시. 멱등)
       8. (첨부 있으면) TEMP -> ATTACHED 로 연결(updateChatMessageAttachments)
       9. 실패시 0.5초 간격 10회 재시도 -> 최종 실패시 DLT(.dlt 토픽)로 이동 + error 로그

B. messageId 설계 : JS-safe Snowflake (53bit)

- DB insert가 비동기가 되면서 auto_increment를 응답에 쓸 수 없음 -> 앱에서 직접 채번.
- 단, 표준 snowflake(63bit)는 JS Number 안전범위(2^53-1)를 넘어서 fe에서 정밀도 깨짐.
  (fe가 messageId/lrm을 숫자로 비교/계산하므로 문자열 변환 대신 53bit로 맞춤)
- 구성 : [timestamp 41bit (custom epoch 2024-01-01 기준 ms)] [workerId 5bit] [sequence 7bit]
  - 41bit ms : 약 69년 사용 가능
  - workerId 5bit : cheg 인스턴스 최대 32대 (application.properties의 chat.snowflake.worker-id)
  - sequence 7bit : 노드당 1ms에 128개 = 노드당 초당 12.8만 msg. 노드 추가로 수평확장.
- 시간순 오름차순 증가 -> 기존 message_id 기반 정렬/페이징/lrm 비교 로직 그대로 사용 가능.
- 기존 auto_increment row들과 공존 : 신규 id가 기존 id보다 훨씬 커서 단조증가 유지됨.

C. Kafka 구성

- topic : castlechat.chat.message (partitions=6, key=roomId)
  - created/deleted/reacted 3종 이벤트가 "한 토픽"에 흐른다. (JsonSerializer의 __TypeId__ 헤더로 타입 구분)
  - 같은 roomId는 항상 같은 파티션 -> 방 단위 순서 보장 -> "생성보다 삭제가 먼저 소비되는" race 원천 차단.
  - DLT : castlechat.chat.message.dlt
- producer : acks=all + enable.idempotence=true (durable save + 중복발행 방지)
  - 발행 후 get(3초) 대기. 이 대기가 "durable save 후 response"의 구현체.
- consumer : group-id=chengine-chat-persist. enable-auto-commit=false (spring-kafka 관리).
  - 재시도 : FixedBackOff 500ms x 10회 -> DeadLetterPublishingRecoverer로 DLT 발행.
  - INSERT IGNORE 라서 재전달(redelivery) 되어도 중복 insert 없음(멱등).

D. 코드 변경 목록 (전부 channel-engine)

1. build.gradle : spring-kafka 추가
2. support/ChatMessageIdGenerator.java : snowflake 채번기 (신규)
3. domain/ChatMessageCreatedEventDTO.java : sendMessage 이벤트 페이로드 (신규)
4. domain/ChatMessageDeletedEventDTO.java : deleteMessage 이벤트 페이로드 (신규)
5. domain/ChatMessageReactedEventDTO.java : reactMessage 이벤트 페이로드 (신규)
6. config/ChatKafkaConfig.java : 토픽 생성 bean + 재시도/DLT 에러핸들러 (신규)
7. kafka/ChatMessageEventPublisher.java : durable 발행기. created/deleted/reacted 3종 발행 (신규)
8. worker/ChatMessagePersistWorker.java : 클래스레벨 @KafkaListener + 타입별 @KafkaHandler 3개로 비동기 DB CRUD (신규)
9. service/ChatCommandService :
   - createChatMessage : DB insert 제거 -> 채번+발행+메모리응답
   - deleteChatMessage : row lock(FOR UPDATE) 제거, 검증(select)만 동기 -> 발행 -> 응답. UPDATE는 consumer가 비동기.
   - reactChatMessage : 동일 패턴. insert/delete는 consumer가 비동기.
10. mapper/ChatMapper.java + mappers/ChatMapperXml.xml :
   - insertChatMessage : useGeneratedKeys 제거, message_id 명시 + INSERT IGNORE (멱등)
   - findChatAttachmentsByIds : 응답용 TEMP 첨부 조회 (신규)
   - deleteChatMessage : 기존 SQL 그대로 사용 (이미 WHERE message_status='ACTIVE' 조건부라 멱등)
11. application.properties : kafka / snowflake workerId / 발행 타임아웃 설정 추가

E. 실패 시나리오 정리

- kafka 브로커 다운 / ack 타임아웃 : client에 "메시지 전송 실패" 에러. 메시지는 어디에도 저장 안 됨(일관성 OK).
- consumer insert 실패(일시적. ex: startDirectChat의 방 생성 tx가 아직 commit 전이라 FK 없음)
  : 재시도(0.5초x10회) 안에 tx commit되면 성공. -> start*Chat 흐름도 안전.
- consumer insert 최종 실패 : DLT에 원본 이벤트 보존 + error 로그. 운영자가 DLT 재처리로 복구.
  (이 경우 client는 이미 성공 응답을 받았으므로, DLT 모니터링이 운영 필수 포인트)
- 중복 전달 : INSERT IGNORE + 사전 확정된 messageId 로 멱등 보장.

F. Kafka 인프라 문서 경계

- Kafka 인프라 정의는 infra/docker-compose.yml을 원본으로 본다.
- 이 문서는 yml/실행명령을 중복 기재하지 않고, 메시지 처리 흐름과 책임 경계만 설명한다.
- 토픽은 앱 기동 시 ChatKafkaConfig의 NewTopic bean이 자동 생성한다.

G. 추후 확장 시 체크포인트 (대용량 전환 시)

- 브로커 3대 이상 + replication.factor=3, min.insync.replicas=2 로 변경 (acks=all의 의미가 진짜 다중화됨)
- 파티션 증설은 key->파티션 매핑이 바뀌므로 신중히. 초기 6개로 시작해 필요시 계획 증설.
- cheg 인스턴스 추가 시 chat.snowflake.worker-id 를 인스턴스마다 다르게(0~31) 부여할 것. 중복 부여 금지.
- DLT 적재량 모니터링(Grafana) + 재처리 배치 도입.

===========================================================================================

H. cheg 내부 데이터 흐름 (파일.메소드 호출 체인)

[용어 정리] "데이터 파이프라인"보다 정확한 표현 :
- gRPC 수신 ~ kafka 발행까지의 동기 구간 = "호출 체인(call chain)"
- kafka 발행 이후의 비동기 구간 = "이벤트 파이프라인(event pipeline)"
- 합쳐서 부를 땐 "메시지 처리 흐름" 정도가 실무 용어에 가까움.

------------------------------------------------------------------------------------------
H-1. SEND_MESSAGE 흐름
------------------------------------------------------------------------------------------

[동기] : 호출 체인. 이 안에서 client 응답까지 끝남

grpc/ChatGrpcEndpoint.createChatMessage
  : gRPC 요청 수신. proto 객체 -> CreateChatMessageCommand 변환만 하고 usecase로 위임. (로직 없음)

-> usecase/ChatCommandUseCase 
(인터페이스)
  : endpoint가 구현체를 직접 모르게 하는 경계. 구현체 = ChatCommandService.

-> service/ChatCommandService.createChatMessage
  : 본체. 순서대로 -
    (1) 인자 검증 (roomId, messageType/messageText/attachmentIds)
    (2) common-redis의 RoomMemberCache.getOrLoadRoomMembers : sender가 방 멤버인지 검증.
        redis hit면 DB 안 감 / miss면 mapper.findAllActiveMemberIdsInRoom(select)로 채움.

-> support/ChatMessageIdGenerator.nextMessageId
  : snowflake 채번. DB/redis 안 가고 메모리에서 messageId 즉시 확정. (H-4 참고)

-> mapper/ChatMapper.findChatAttachmentsByIds
 (첨부 있을 때만)
  : 응답에 실을 첨부 정보 select (읽기 전용. TEMP row). "쓰기"는 여기서 절대 안 함.

-> kafka/ChatMessageEventPublisher.publishChatMessageCreated
  : ChatMessageCreatedEventDTO를 토픽 "castlechat.chat.message"에 발행(key=roomId).
    acks=all 디스크 기록 완료(durable save)까지 get(3초) 대기. <== prpr 3의 "response 시점" 기준선.
    실패/타임아웃이면 여기서 예외 -> client는 전송실패를 받고, 메시지는 어디에도 안 남음.

-> (다시 ChatCommandService로 복귀)
  : common-redis의 RoomReadPositionCache.updateIfGreater로 sender 본인 lrm 갱신(redis만)
    
-> mapper.findChatMessageNotificationTargetUserIds(select)로 알림대상 조회

-> DB insert 결과가 아닌 "메모리의 값들"로 ChatMessageViewResponseDTO 조립 -> return

-> grpc/ChatGrpcEndpoint : DtoToGrpcConverter로 proto 변환 -> wsgate로 응답 나감. [client 응답 완료]

[비동기] : 이벤트 파이프라인. 응답과 무관하게 뒤에서 진행

kafka broker (castle-chatKafka 컨테이너)
  : 이벤트를 디스크에 보관. cheg가 죽어도 여기 남아있음(복구 기준점).

-> worker/ChatMessagePersistWorker.persistChatMessage 
(@KafkaHandler)
  : ChatMessageCreatedEventDTO 수신 
  
->
    (1) mapper.insertChatMessage : INSERT IGNORE (messageId 명시. 재전달 와도 멱등)
    (2) 첨부 있으면 mapper.updateChatMessageAttachments : TEMP -> ATTACHED 연결
    (3) 성공시 offset 커밋. 실패시 ChatKafkaConfig의 에러핸들러가 0.5초x10회 재시도 -> DLT.

------------------------------------------------------------------------------------------
H-2. DELETE_MESSAGE 흐름 (SEND와 구조 동일. 검증 내용만 다름)
------------------------------------------------------------------------------------------

[동기]
grpc/ChatGrpcEndpoint.deleteChatMessage -> usecase -> service/ChatCommandService.deleteChatMessage
  : (1) 인자 검증
    (2) mapper.findChatMessageStatus (select) : 존재여부 + 이미 삭제됐는지 검증
    (3) mapper.findChatMessageSenderUserId (select) : 작성자 본인인지 검증
    ※ 기존의 FOR UPDATE row lock은 제거됨. 검증~반영 사이의 경합은 consumer의 조건부 UPDATE가 멱등 흡수.
-> kafka/ChatMessageEventPublisher.publishChatMessageDeleted : durable save 대기
-> service : DeleteChatMessageResponseDTO 조립 -> return. [client 응답 완료]

[비동기]
-> worker/ChatMessagePersistWorker.persistDeleteChatMessage (@KafkaHandler)
  : mapper.deleteChatMessage = UPDATE ... WHERE message_status='ACTIVE' (조건부라 멱등)
    affected 0이면 "이미 삭제됨/재전달"로 보고 정상 종료(재시도 안 함).

※ 순서 보장 : created와 deleted가 같은 토픽+같은 key(roomId) = 같은 파티션이라,
   "insert 되기 전에 delete가 먼저 소비되는" 역전이 구조적으로 불가능.

------------------------------------------------------------------------------------------
H-3. REACT_MESSAGE 흐름 (DELETE와 동일 패턴)
------------------------------------------------------------------------------------------

[동기]
grpc/ChatGrpcEndpoint.reactChatMessage -> usecase -> service/ChatCommandService.reactChatMessage
  : (1) 인자 검증
    (2) mapper.findChatMessageStatus (select) : 존재/삭제여부 검증
    (3) RoomMemberCache.getOrLoadRoomMembers (redis) : 방 멤버인지 검증
-> kafka/ChatMessageEventPublisher.publishChatMessageReacted : durable save 대기
-> service : ReactChatMessageEventResponseDTO 조립 -> return. [client 응답 완료]

[비동기]
-> worker/ChatMessagePersistWorker.persistReactChatMessage (@KafkaHandler)
  : addRequested=true -> mapper.insertChatMessageReaction (INSERT IGNORE. 멱등)
    addRequested=false -> mapper.deleteChatMessageReaction (DELETE. 멱등)
    affected 0이면 재전달/경합으로 보고 정상 종료.

------------------------------------------------------------------------------------------
H-4. 각 파일의 역할 한줄 요약
------------------------------------------------------------------------------------------

grpc/ChatGrpcEndpoint        : gRPC 문지기. proto<->command 변환만. 비즈니스 로직 금지.
usecase/ChatCommandUseCase   : 인터페이스 경계. endpoint가 service 구현체를 직접 모르게 함.
service/ChatCommandService   : 비즈니스 로직 본체. "검증(동기) -> 채번 -> 발행(durable) -> 응답조립".
support/ChatMessageIdGenerator: snowflake 채번기. 유일하게 상태(sequence)를 가진 유틸.
domain/*EventDTO (3종)       : kafka에 실려가는 이벤트 모양. producer/consumer의 계약서.
kafka/ChatMessageEventPublisher: 발행 전담. "durable save 후 response"(prpr 3)의 구현 지점.
worker/ChatMessagePersistWorker: 소비 전담. DB CRUD가 실제로 일어나는 유일한 곳(메시지 3종 기준).
config/ChatKafkaConfig       : 토픽 정의 + 실패정책(재시도/DLT). 흐름엔 안 끼지만 규칙을 정함.
mapper/ChatMapper(+Xml)      : SQL 저장소. service에서는 select만, worker에서는 쓰기만 호출됨.
common-redis RoomMemberCache / RoomReadPositionCache : 검증/lrm의 redis 캐시. DB 방어벽.
