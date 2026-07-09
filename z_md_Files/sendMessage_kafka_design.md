< sendMessage Kafka 비동기 아키텍처 설계 문서 >

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
       4. Kafka publish "castlechat.chat.message.created" (key=roomId)
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

- topic : castlechat.chat.message.created (partitions=6, key=roomId)
  - 같은 roomId는 항상 같은 파티션 -> 방 단위 메시지 순서 보장
  - DLT : castlechat.chat.message.created.dlt
- producer : acks=all + enable.idempotence=true (durable save + 중복발행 방지)
  - 발행 후 get(3초) 대기. 이 대기가 "durable save 후 response"의 구현체.
- consumer : group-id=chengine-chat-persist. enable-auto-commit=false (spring-kafka 관리).
  - 재시도 : FixedBackOff 500ms x 10회 -> DeadLetterPublishingRecoverer로 DLT 발행.
  - INSERT IGNORE 라서 재전달(redelivery) 되어도 중복 insert 없음(멱등).

D. 코드 변경 목록 (전부 channel-engine)

1. build.gradle : spring-kafka 추가
2. support/ChatMessageIdGenerator.java : snowflake 채번기 (신규)
3. dto/ChatMessageCreatedEventDTO.java : kafka 이벤트 페이로드 (신규)
4. config/ChatKafkaConfig.java : 토픽 생성 bean + 재시도/DLT 에러핸들러 (신규)
5. kafka/ChatMessageEventPublisher.java : durable 발행기 (신규)
6. worker/ChatMessagePersistWorker.java : @KafkaListener 비동기 DB insert (신규)
7. service/ChatCommandService.createChatMessage : DB insert 제거 -> 채번+발행+메모리응답
8. mapper/ChatMapper.java + mappers/ChatMapperXml.xml :
   - insertChatMessage : useGeneratedKeys 제거, message_id 명시 + INSERT IGNORE (멱등)
   - findChatAttachmentsByIds : 응답용 TEMP 첨부 조회 (신규)
9. application.properties : kafka / snowflake workerId / 발행 타임아웃 설정 추가

E. 실패 시나리오 정리

- kafka 브로커 다운 / ack 타임아웃 : client에 "메시지 전송 실패" 에러. 메시지는 어디에도 저장 안 됨(일관성 OK).
- consumer insert 실패(일시적. ex: startDirectChat의 방 생성 tx가 아직 commit 전이라 FK 없음)
  : 재시도(0.5초x10회) 안에 tx commit되면 성공. -> start*Chat 흐름도 안전.
- consumer insert 최종 실패 : DLT에 원본 이벤트 보존 + error 로그. 운영자가 DLT 재처리로 복구.
  (이 경우 client는 이미 성공 응답을 받았으므로, DLT 모니터링이 운영 필수 포인트)
- 중복 전달 : INSERT IGNORE + 사전 확정된 messageId 로 멱등 보장.

F. 로컬 Kafka 설치 방법 (KRaft 단일 브로커. Zookeeper 불필요)

infra/docker-compose.yml 에 정의되어 있음. (monitoring/은 관찰 전용이므로 앱 인프라는 infra/로 분리)

```yaml
  chatKafka:
    image: apache/kafka:3.9.0
    container_name: castle-chatKafka
    ports:
      - "9094:9094"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@chatKafka:9093
      KAFKA_LISTENERS: INTERNAL://chatKafka:9092,CONTROLLER://chatKafka:9093,EXTERNAL://0.0.0.0:9094
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://chatKafka:9092,EXTERNAL://localhost:9094
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
    mem_limit: 768m
```

- 실행 : infra 폴더에서 `docker compose up -d chatKafka`
- 확인 : `docker exec castle-chatKafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server chatKafka:9092 --list`
- 토픽은 앱 기동 시 ChatKafkaConfig의 NewTopic bean이 자동 생성함(수동 생성 불필요).
- cheg의 application.properties는 localhost:9094 로 접속하도록 설정되어 있음.

G. 추후 확장 시 체크포인트 (대용량 전환 시)

- 브로커 3대 이상 + replication.factor=3, min.insync.replicas=2 로 변경 (acks=all의 의미가 진짜 다중화됨)
- 파티션 증설은 key->파티션 매핑이 바뀌므로 신중히. 초기 6개로 시작해 필요시 계획 증설.
- cheg 인스턴스 추가 시 chat.snowflake.worker-id 를 인스턴스마다 다르게(0~31) 부여할 것. 중복 부여 금지.
- DLT 적재량 모니터링(Grafana) + 재처리 배치 도입.
