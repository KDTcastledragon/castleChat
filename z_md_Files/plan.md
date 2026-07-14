< castleChat 채팅웹 프로그램 기획 문서 >

====== A. priority principle (===prpr) [prpr은 프로젝트의 모든 곳에서 구현 전에 반드시 철저히 검증하고 지켜야한다. 나의 질문에 답할때도 무조건 아래의 원칙에 기반하여 답변해라. 1차 MVP 기준으로 답변하지말고, 완성본 기준으로 답변해라.]=======================================

1. 대용량 트래픽에 대응 가능한 설계 구조 + 실무적 관점에 맞는 설계 + Client의 쾌적한 UX => 이 3가지가 최우선순위 원칙이다. 이 원칙을 위배하는 설계는 무조건 틀린 설계이므로, 구현조차 하지마라. 니가 반드시 지켜야할 원칙이자 이 프로젝트의 최우선 목표다.
2. UX관련 기능부문은 Redis , batch , kafka 를 이용한 빠른처리 + 비동기 처리를 적극 활용한다. 둘을 반드시 함께 사용하라는 얘기는 아니다. 필요한 곳에 적절하게 단독/혼합 사용할 것.   
3. sendMessage는 channel-engine process의 @Service에서, DB insert후 response하지 않고, kafka에서 durable save후에 response한다. DB insert는 kafka에서 "비동기"로 묶어서 처리한다.
4. readMessage는 절대로 DB에 대한 CRUD행위를 "동기"로 처리하지 않는다.(client의 read 요청:db select/update 행위 = 1:n(n>=1자연수) 구조가 되면 절대로 안된다.) Redis를 적극적으로 활용하며, updateLastReadMessageId(===lrm) 은 반드시 batch로 "비동기" 처리한다.(현재 dirty flush worker 사용중.)
5. variable , constant , function , method 등의 naming은 "무조건" 나의 스타일에 맞춰서 한다. 니 마음대로 naming 하지 말 것.
6. 내가 rough하게/애매하게 물어볼 경우, "~~의도가 담긴 질문이 맞나요? / ~ 한 질문이 맞나요?" 라고 반드시 나에게 되물어봐라.
7. 현재 나는 기업체가 아닌 취업준비생으로서, AI Assist 파트는 비용 문제 때문에 현실적으로 많은 유저들이 많이 사용하게 하는건 불가능한 상태이다. 참고해라.
8. gradle refresh , project clean , .\gradlew.bat 실행은 내가 너에게 "직접 지시하는 경우"를 제외하고는 실행하지 않는다. 내가 직접한다.
9. 정상작동하던 기능들은 "너의 임의대로" 건드리지 않는다. 건드려야한다면, 반드시 나에게 "~~~한 이유 때문에  ---~한 부분을 수정해야하는데 고쳐도될까요?" 라고 물어보고 진행할 것.
10. 명령 수행시, 각 항목마다 ctxt.md에 원인과 해결방법을 자세히 기록할 것.
11. ctxt.md파일에 기록시 
=========================< [ 수행한 AI이름 ] >=============================================
을 작성 후에 기록할 것. 하단 예시 참고해라.

- CLAUDE가 수행했을 경우 ↓
=========================< [ CLAUDE ] 26.07.30 / 15:30:22 >=============================================

- CODEX가 수행했을 경우 ↓
=========================< [ CODEX ] 26.07.30 / 15:30:22 >==============================================

11. context-notes.md를 참고하며 진행할 것.

====== B-1. 구현해야 할 기능 1 (websocket기반)=====================================================================

[비동기 정책] 메시지 4종(SEND/READ/DELETE/REACT_MESSAGE)만 kafka/batch 비동기 처리한다.
나머지 기능은 전부 "동기"로 처리한다. 이유 : 저빈도 + 실패를 즉시 알려줘야 하는 기능들(권한없음, 이미 친구임 등)이라
비동기로 얻는 이득이 없고 일관성만 깨진다. (단, attachment 업로드는 SEND 이전 단계의 동기 REST. 13번 참고)

gCli-chEdP : wsgate gRPC client -> cheg gRPC endpoint 구간의 호출 구조는 거의 그대로 유지한다. 바뀐 핵심은 endpoint 이후 cheg.service 내부 처리다.
SEND/START 계열은 DB insert 결과(auto_increment)를 기다리지 않고, Snowflake messageId 채번 + Kafka durable save 후 메모리 값으로 response를 조립한다.
response에는 즉시 렌더링용 messageId/createdAt/attachments/notificationTargetUserIds가 실리며, 실제 chat_messages insert와 attachment TEMP->ATTACHED 연결은 kafka consumer가 비동기로 처리한다.

1. ADD_FRIEND : 친구요청. 'PENDING' row 생성.
   fe emitWsAddFriend -> wsgate.WsGateFriendHandler.handleAddFriend(세션검증) -> gRPC -> cheg.FriendCommandService.addFriend(db insert)
   -> 요청자에게 responseOk "ADD_FRIEND_OK" + 대상자에게 pushToSingleUser "FRIEND_REQUEST_RECEIVED" (온라인이면 실시간 알림).

2. RESPOND_FRIEND : 나에게 'PENDING'한 ADD요청에 대한 응답. 'ACCEPT' 'REJECT' 준다.
   fe emitWsRespondFriend -> wsgate.handleRespondFriend -> gRPC -> cheg.FriendCommandService.respondFriend(db update)
   -> 응답자에게 responseOk "RESPOND_FRIEND_OK" + 원래 요청자에게 push "FRIEND_REQUEST_RESPONDED".

3. BLOCK_FRIEND : ADD , ACCEPT , REJECT와 성격이 완전 달라서 따로 구현 필요. **UNBLOCK도 이 메소드로 한꺼번에 처리하는 것도 괜찮을지. (내 생각이 틀리다면 이 항목 하단에 Cmt : ~~ 방식으로 의견 적어라.)
   Cmt : 동의함(틀리지 않음). REACT_MESSAGE의 addRequested 패턴처럼 blockRequested 플래그 하나로 BLOCK/UNBLOCK 처리 = 기존 스타일과 일관.
   단 BLOCK은 부수효과가 큼(PENDING 요청 자동거절, 메시지 수신차단 등) -> API는 하나, service 내부 분기는 커져도 OK.

4. OPEN_DIRECT_CHAT : 친구목록(!==채팅목록)에서만 사용할 기능. 메시지 없이 1:1 방만 열어서 보여줌.
   fe emitWsOpenDirectChat -> wsgate.WsGateRoomHandler.handleOpenDirectChat -> gRPC -> cheg.RoomCommandService.openDirectChatRoom
   -> 방 있으면 기존방 정보, 없으면 draft 상태로 반환 -> 세션 방 등록(enterRoomSession) + responseOk "OPEN_DIRECT_CHAT_OK".
   (실제 방 생성은 첫 메시지 전송 = START_DIRECT_CHAT 시점)

5. START_DIRECT_CHAT : 친구목록/draft 채팅창에서 "첫 메시지 전송"으로 1:1 방 개설+메시지를 한번에 처리.
   [흐름 - 실제 코드 기준]
   (1) fe : wsClient.emitWsStartDirectChat(targetPublicId, messageText, attachmentIds...) 전송.
   (2) wsgate.WsGateChatHandler.handleStartDirectChat :
       - wsGateAuth.requireLoginUser 세션 로그인 검증
       - payload 검증 (targetPublicId 누락 / 메시지 내용 없음 -> 즉시 responseFail)
       - StartDirectChatCommand 조립 -> wsGateChatClient.startDirectChat (gRPC로 cheg 호출)
   (3) cheg.ChatGrpcEndpoint.startDirectChat -> ChatCommandService.startDirectChat (@Transactional) :
       - 인자 검증 + target/sender 유저 조회 (db select)
       - 기존 DIRECT 방 있으면 재사용 : 멤버 reactivate + RoomMemberCache 보정
         없으면 신규 : 방 create + 멤버 2명 insert + RoomMemberCache init
       - createChatMessage 호출 = SEND_MESSAGE와 완전히 동일한 파이프라인
         (snowflake 채번 -> kafka durable save -> redis lrm updateIfGreater -> 메모리값으로 response 조립)
       - buildDirectEnterRoomInfo (방 정보+멤버목록+공지 select) -> StartChatResponseDTO(enterRoomInfo, firstChatMessage) 반환
   (4) wsgate (gRPC 응답 수신 후) :
       - wsGateSessionRegistry.enterRoomSession : 요청자 세션을 방에 등록
       - responseOk "START_DIRECT_CHAT_OK" : 요청자에게 방정보+첫메시지
       - broadcastToRoom "MSG_CREATED" : 방을 보고있는 세션들에게 첫 메시지 전파
       - pushChatMessageNotification : 알림 대상 유저에게 푸시
   [주의점] 방 생성 tx가 commit 되기 전에 kafka consumer가 첫 메시지 insert를 먼저 시도할 수 있음(FK 없음 실패)
   -> consumer 재시도(0.5초x10회)가 흡수. 정상 동작임.

6. START_GROUP_CHAT : 첫 메시지 전송으로 그룹방 개설+초대+메시지를 한번에 처리.
   fe emitWsStartGroupChat(inviteMemberPublicIds, roomName...) -> wsgate.handleStartGroupChat -> gRPC -> cheg.ChatCommandService.startGroupChat(@Transactional)
   -> 초대멤버 검증(전원 실존해야함) -> 방 create + HOST(개설자)/MEMBER(초대자들) insert + RoomMemberCache init
   -> createChatMessage(SEND_MESSAGE와 동일 파이프라인 : snowflake -> kafka durable save -> 응답)
   -> wsgate : 세션 방 등록 + responseOk + broadcast "MSG_CREATED" + 알림 push.

7. ENTER_ROOM : 채팅목록에서 방 클릭시 입장(=채팅창 열기). 방 정보+멤버+공지+메시지 로드용.
   fe emitWsEnterRoom -> wsgate.handleEnterRoom -> gRPC -> cheg.RoomCommandService.enterRoom(방 정보 조회, lrm warmUp)
   -> 세션 방 등록(enterRoomSession = 이 유저가 이 방을 "보는 중" 표시) -> responseOk "ENTER_ROOM_OK".

8. EXIT_ROOM : 채팅창 닫기. 방 멤버 탈퇴가 아님!
   cheg까지 안 감. wsgate단에서 wsGateSessionRegistry.exitRoomSession(세션-방 연결만 해제)만 하고 responseOk.
   (TYPING과 같은 이유 : business logic 없음. db 접근 없음.)

9. LEFT_ROOM : 방을 진짜 나감(멤버 탈퇴). EXIT_ROOM과 완전히 다름.
   fe emitWsLeftRoom -> wsgate.handleLeftRoom -> gRPC -> cheg.RoomCommandService.leftRoom(member_status 변경 + RoomMemberCache 제거)
   -> 세션 방 해제 + 방에 broadcast "LEFT_ROOM"(누가 나갔는지 피드) + responseOk.

10. TYPING_START/STOP : cheg까지 안가고 wsgate단에서 처리. 실제 busniess logic이 없기 때문.

11. SEND_MESSAGE : websocket(===ws) 기반 구현. db insert 는 kafka로 비동기처리. channel-engine(===cheg)이 response하는 시점은, db insert후가 아닌, kafka에서 durable save 후다. **kafka durable save 후 response하기 직전에 server가 터져버려서 batch db insert가 실패 되었을시, 어떻게 복구해야할까?
    답 : 유실 안됨. 이벤트는 이미 kafka 디스크에 있고, consumer offset은 "DB insert 성공 후" 커밋되므로
    cheg 재기동시 미처리 이벤트부터 자동 재소비 -> INSERT IGNORE 멱등이라 중복도 안전. DB 장애 지속시엔 재시도 10회 -> DLT 보관 -> 복구 후 재처리.
    남는 구멍은 client측(응답 못받아서 재전송하면 중복 메시지) -> 완성본에서 fe가 clientMessageId(멱등키) 보내고 cheg가 redis dedup 하는 보완 필요. [추후 구현 항목]

12. READ_MESSAGE : read처리 비동기로 한다. cheg.service->reids-> cheg.worker의 dirty flush worker가 비동기로 처리함.
lrm기반으로 Front-end(===fe)에서 unreadCount(===urc. 해당 Message를 읽지 않은 사람수)를 계산하며, urc는 db에 저장하지 않는다. 실제 fe의 urc계산은 oldLrm과 lrm(최신Lrm의미)으로 계산함.

13. ATTACHMENT_UPLOAD : SEND_MESSAGE "이전"에 일어나는 별도 동기 REST 단계. 비동기 금지.
    fe가 파일 선택 -> domain-service REST(uploadChatAttachments)로 동기 업로드 -> 파일 저장 + TEMP row 생성 -> attachmentId들 반환
    -> fe가 그 attachmentId들을 SEND_MESSAGE에 실어보냄. TEMP->ATTACHED 연결(update)은 kafka consumer가 비동기 처리(이미 구현됨).
    업로드 자체를 비동기로 하면 안되는 이유 : 응답 시점에 파일이 실제 저장됐다는 보장이 없어 fileUrl이 깨진 이미지가 되고,
    업로드 진행률 UX도 불가능해짐. "파일저장=동기 / 메시지연결=비동기" 이 분리가 정답.

14. DELETE_MESSAGE : **얘도 db update할때 kafka로 비동기 처리해도 되지 않나....?
    답 : 가능, 완성본 기준 권장. 같은 토픽(key=roomId)에 태우면 send->delete 순서가 파티션 내에서 보장되어 race 원천차단.
    검증(작성자/상태)은 동기 select, UPDATE는 조건부(WHERE message_status='ACTIVE')로 멱등화. [구현 완료. 실기동 검증 대기]
    gCli-chEdP : 변경 없음. wsgate(핸들러/gRPC client)와 cheg의 ChatGrpcEndpoint는 시그니처/DTO 그대로.
    바뀐 건 cheg.ChatCommandService "내부"부터 (row lock 제거 -> 검증 select만 동기 -> kafka 발행 -> 응답. UPDATE는 worker가 비동기).

15. REACT_MESSAGE : **얘도 db insert/delete할때 kafka로 비동기...처리 하자. ux를 위해.
    결정 : 전환한다. DELETE와 같은 방식(검증 동기 -> kafka 발행 -> 응답, consumer가 insert/delete). 이미 INSERT IGNORE 멱등이라 깔끔. [구현 완료. 실기동 검증 대기]
    gCli-chEdP : 변경 없음 (DELETE와 동일한 이유).

16. INVITE_MEMBER : 그룹방에 멤버 초대.
    fe emitWsInviteMember -> wsgate.handleInviteMember -> gRPC -> cheg.RoomCommandService.inviteMember(권한검증 + member insert + RoomMemberCache 추가)
    -> responseOk + 방 broadcast(입장 피드) + 초대된 유저에게 push.

17. KICK_MEMBER : 강퇴. HOST/관리자 권한 필요.
    fe emitWsKickMember -> wsgate.handleKickMember -> gRPC -> cheg.RoomCommandService.kickMember(권한검증 + member_status 변경 + RoomMemberCache 제거)
    -> responseOk + 방 broadcast + 강퇴된 유저 세션 방 해제. 동기 필수(강퇴됐는데 방이 계속 보이면 안됨).

18. BAN_MEMBER : 차단(강퇴+재입장 금지). KICK과 동일 흐름, member_status만 다름(BANNED).
    cheg.RoomCommandService.banMember. 동기 필수(KICK과 같은 이유).

19. APPLY_ROOM_NOTICE : 방 공지 등록/변경.
    fe -> wsgate.handleApplyRoomNotice -> gRPC -> cheg.RoomCommandService.applyRoomNotice(권한검증 + notice insert/update)
    -> responseOk + 방 broadcast(공지 변경 실시간 반영).

20. CHANGE_MEMBER_ROLE : 멤버 역할 변경(HOST/MEMBER 등). HOST 권한 필요.
    fe -> wsgate.handleChangeMemberRole -> gRPC -> cheg.RoomCommandService.changeMemberRole(권한검증 + role update)
    -> responseOk + 방 broadcast.


