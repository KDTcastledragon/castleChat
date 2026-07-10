# trouble shooting

## 2026-07-10 채팅방/공지/방 설정 오류 수정

### 1. 그룹방 만들 때 자동 메시지가 보내지던 문제

원인은 `Friends.jsx -> useChatRoom.createGroupRoom` 흐름에서 단톡방 생성 버튼을 누르는 순간 `firstMessageText = "...생성되었습니다."`를 만든 뒤 `emitWsStartGroupChat`을 호출하고 있었기 때문이다.

이 구조는 `START_GROUP_CHAT`의 의미인 "첫 메시지 전송으로 방 생성"을 단톡방 생성 버튼에서 먼저 실행해버린다. 그래서 host가 직접 메시지를 입력하지 않았는데도 DB room 생성과 메시지 생성이 같이 일어났다.

수정 내용.

- `castlechat/src/hooks/useChatRoom.js`.
  - `createGroupRoom`이 더 이상 `emitWsStartGroupChat`을 호출하지 않도록 변경했다.
  - 선택된 친구와 방 이름/썸네일만 들고 `GROUP draft chat window`를 연다.
  - 실제 DB room 생성은 `ChatBox`에서 host가 첫 메시지를 보낼 때만 `emitWsStartGroupChat`으로 처리한다.
- `castlechat/src/components/Chattings/ChatBox.jsx`.
  - `isDraftGroupRoom`을 추가했다.
  - draft 그룹방에서 전송 버튼을 누르면 그때 `emitWsStartGroupChat(localRoomName, roomThumbnailUrl, inviteMemberPublicIds, chatMessage)`를 호출한다.
- `castlechat/src/store/chatWindowsSlice.js`, `castlechat/src/components/AppShell/AppShell.jsx`.
  - draft group chat에 필요한 `inviteMemberPublicIds`를 창 state와 props로 전달하게 했다.
- `castlechat/src/components/Home/Friends.jsx`.
  - 버튼 문구를 `단톡초대하기`에서 `단톡방 열기`로 변경했다.

결과 정책.

- 단톡 만들기 버튼은 DB room을 만들지 않는다.
- host가 draft 채팅창에서 실제 첫 메시지를 보낼 때만 room/message가 생성된다.

### 2. AI 추천 `PathVariable Long roomId` 오류

오류 메시지.

```text
Name for argument of type [java.lang.Long] not specified, and parameter name information not available via reflection.
```

원인은 `AiRecommendController.recommendMessages(@PathVariable Long roomId, ...)`에서 path variable 이름을 명시하지 않았기 때문이다. Java 컴파일 옵션에 `-parameters`가 없으면 Spring이 메소드 파라미터명 `roomId`를 reflection으로 알 수 없다.

수정 내용.

- `castledragon/ai-assistant-service/src/main/java/com/chat/aiassist/controller/AiRecommendController.java`.
  - `@PathVariable Long roomId`를 `@PathVariable("roomId") Long roomId`로 변경했다.

### 3. 메시지 전송/읽음 시 `img src=""` 경고

오류 메시지.

```text
An empty string ("") was passed to the src attribute.
```

원인은 `sender?.profileImg ?? '/images/mococo_question.png'`가 빈 문자열을 fallback 처리하지 못했기 때문이다. `??`는 `null`/`undefined`만 대체하고 `""`는 그대로 통과시킨다.

수정 내용.

- `castlechat/src/components/Chattings/ChatBox.jsx`.
  - `sender?.profileImg ?? ...`를 `sender?.profileImg || ...`로 변경했다.

결과.

- 프로필 이미지가 빈 문자열이면 기본 이미지 `/images/mococo_question.png`를 사용한다.

### 4. 알림 on/off 및 방 설정 저장 시 500 오류

원인은 `domain-service`의 `RoomMapperXml.updateMyRoomSettings`에서 컬럼명을 `custom_room_back_ground`로 쓰고 있었기 때문이다. 현재 다른 mapper들은 `custom_room_background`를 기준으로 조회/삽입하고 있다.

수정 내용.

- `castledragon/domain-service/src/main/resources/mappers/RoomMapperXml.xml`.
  - `custom_room_back_ground`를 `custom_room_background`로 변경했다.
- `castledragon/channel-engine/src/main/resources/mappers/RoomMapperXml.xml`.
  - invite member insert 쪽에 남아있던 `custom_room_back_ground`도 `custom_room_background`로 변경했다.

추가 수정.

- `ChatBox`에서 방 설정 이미지 업로드가 `sendFileApi`를 타고 있었다.
- 이 API는 메시지 첨부용 `chat_message_attachments TEMP row`를 만드는 흐름이므로 방 썸네일/배경에 부적절하다.
- `uploadImageApi(file, 'ROOM_THUMBNAIL' | 'ROOM_BACKGROUND')`를 사용하도록 바꿨다.

### 5. 메시지를 공지로 올릴 때 숫자 alert만 뜨던 문제

원인은 `ChatBox.handleMessageMenuAction('NOTICE')`가 실제 WS 요청을 보내지 않고 `alert("공지: messageId")`만 실행하고 있었기 때문이다.

수정 내용.

- `castlechat/src/webSocket/wsClient.js`.
  - `APPLY_ROOM_NOTICE` ws type과 `emitWsApplyRoomNotice`를 추가했다.
- `castlechat/src/components/Chattings/ChatBox.jsx`.
  - 우클릭 메뉴의 `공지` 액션이 `emitWsApplyRoomNotice`를 호출하게 변경했다.
  - 메시지 공지는 `roomNoticeAction='CREATE'`, `roomNoticeType='MESSAGE'`, `sourceMessageId`, `roomNoticeContents`를 보낸다.
  - `ROOM_NOTICE_APPLIED` broadcast를 받으면 시스템 피드와 현재 공지 상태를 갱신한다.

주의.

- 백엔드는 `RoomNoticeApplyResponseDTO`의 feed 필드명이 `roomFeedResponse`다.
- FE 수신부도 `notice.roomFeedResponse ?? notice.roomFeed ?? notice` 순서로 맞췄다.

### 6. 방 이름/썸네일/배경이 저장 전에 즉시 바뀌던 문제

원인은 `localRoomName`, `roomThumbnailUrl`, `roomBackgroundUrl`이 화면 적용값과 설정창 입력값 역할을 동시에 하고 있었기 때문이다.

수정 내용.

- `castlechat/src/components/Chattings/ChatBox.jsx`.
  - 적용값.
    - `localRoomName`
    - `roomThumbnailUrl`
    - `roomBackgroundUrl`
  - 설정창 임시값.
    - `settingRoomName`
    - `settingRoomThumbnailUrl`
    - `settingRoomBackgroundUrl`
  - 저장 버튼 성공 후에만 임시값을 적용값으로 복사한다.
  - 방 이름은 기본적으로 disabled 상태이며, `변경` 버튼을 눌렀을 때만 input을 수정할 수 있다.
  - `방 설정` 텍스트는 제거했다.
- `castlechat/src/components/Chattings/ChatBox.css`.
  - 방 이름 변경 버튼, disabled input, 파일명 표시 UI를 추가했다.

결과.

- 방 이름/썸네일/배경은 선택 즉시 화면에 적용되지 않는다.
- `방 설정 저장` 성공 후에만 채팅창과 DB에 반영된다.

### 7. 채팅방 메뉴에서 선택한 썸네일/배경 파일명이 안 보이던 문제

원인은 파일 선택 후 UI가 URL 존재 여부만 보고 `선택됨`이라고만 표시했기 때문이다.

수정 내용.

- `castlechat/src/components/Chattings/ChatBox.jsx`.
  - `settingRoomThumbnailFileName`, `settingRoomBackgroundFileName` state를 추가했다.
  - 파일 선택 후 실제 파일명을 표시한다.

### 8. 채팅방 메뉴에 공지사항 handling 메뉴가 없던 문제

수정 내용.

- `castlechat/src/components/Chattings/ChatBox.jsx`.
  - 방 메뉴 안에 `공지사항` 영역을 추가했다.
  - 현재 공지가 있으면 내용 표시.
  - ACTIVE 공지는 `내리기`.
  - INACTIVE 공지는 `재공지`.
  - 공통으로 `삭제`.
- `castlechat/src/components/Chattings/ChatBox.css`.
  - 공지 영역 스타일 추가.

현재 범위.

- 메시지를 공지로 등록하는 흐름은 우클릭 `공지`에서 처리한다.
- 직접 작성 CUSTOM 공지는 아직 별도 입력 UI까지는 만들지 않았다.

### 9. 채팅방 목록에 방 썸네일이 안 뜨던 문제

원인은 DB/DTO에는 `customRoomThumbnail`이 내려오는데 `ChatList.jsx`에서 렌더링하지 않았기 때문이다.

수정 내용.

- `castlechat/src/components/Chattings/ChatList.jsx`.
  - 각 방 row 앞에 `customRoomThumbnail` 이미지를 추가했다.
  - 값이 없으면 `/images/mococo_question.png` 사용.
- `castlechat/src/components/Chattings/ChatList.css`.
  - `.chatListRoomThumbnail` 스타일 추가.

### 확인 필요

이번 수정은 코드/정적 검색 기준으로 반영했다. 실제 런타임 검증은 아래 기능 순서로 확인하면 된다.

1. 단톡 만들기 버튼 클릭 시 DB room/message가 바로 생성되지 않는지 확인.
2. draft 단톡창에서 host가 첫 메시지를 보낼 때 room/message가 생성되는지 확인.
3. AI 추천 버튼 클릭 시 PathVariable 오류가 사라지는지 확인.
4. 메시지 전송 후 `src=""` warning이 사라지는지 확인.
5. 알림 on/off 저장이 500 없이 성공하는지 확인.
6. 방 이름/썸네일/배경이 저장 버튼 후에만 적용되는지 확인.
7. 메시지 우클릭 공지가 실제 공지로 올라가는지 확인.
8. 채팅방 목록에 썸네일이 보이는지 확인.

## 2026-07-10 카카오톡 스타일 공지·알림·리액션 개선

### 1. 활성 공지 상단 고정과 숨김 토글

문제.

- 활성 공지가 채팅방 데이터에는 있었지만 메시지 영역 상단에 고정 표시되는 UI가 없었다.
- 공지를 잠깐 숨기는 UI 상태와 서버의 `ACTIVE/INACTIVE` 상태를 구분할 필요가 있었다.

수정 내용.

- `castlechat/src/components/Chattings/ChatBox.jsx`.
  - 활성 공지를 `[닉네임] : [공지내용]` 형식으로 상단에 고정했다.
  - 닉네임과 내용은 각각 말줄임 처리한다.
  - `숨기기/표시하기` 토글을 추가했다.
  - 숨김은 해당 채팅창의 UI 상태만 변경하며 공지를 실제로 내리지 않는다.
- `castlechat/src/components/Chattings/ChatBox.css`.
  - 상단 공지 바와 축소 상태 스타일을 추가했다.

### 2. 공지사항 이력 패널과 20개 커서 조회

문제.

- 서버와 FE 모두 현재 활성 공지 하나만 취급해 과거 공지를 볼 방법이 없었다.
- 채팅방 메뉴가 공지 내용과 내리기/삭제 버튼을 직접 노출해 공지 이력 진입점 역할을 하지 못했다.

수정 내용.

- `domain-service`에 `GET /room/{roomId}/notices`를 추가했다.
- `beforeRoomNoticeId` 기준 최신순 20개 커서 페이지를 사용한다.
- 활성 방 멤버인지 확인한 후에만 이력을 반환한다.
- 삭제 공지는 DB 원문을 유지하지만 FE에는 `삭제된 공지사항입니다.`로 표시한다.
- 상단 공지 또는 채팅방 메뉴의 공지를 누르면 전체 공지 패널이 열린다.
- 스크롤 하단 도달 시 이전 공지 20개를 추가 조회한다.
- 작성자에게만 수정/내리기/재공지/삭제 버튼을 표시한다.
- ESC 입력 시 공지 패널을 먼저 닫는다.
- 채팅방 메뉴에서는 직접 액션 버튼을 제거하고 공지 패널 진입만 제공한다.

### 3. 새 공지·내림·삭제 반영 실패와 gRPC UNKNOWN

원인.

- `ROOM_NOTICE_APPLIED` payload의 roomId는 `payload.roomNoticeView.roomId`였지만 `wsClient.js`가 `payload.roomId`만 읽었다. 성공한 broadcast가 방 핸들러까지 전달되지 않았다.
- 새 공지가 기존 활성 공지를 교체할 때 기존 활성 공지의 작성자 소유권까지 검사했다. 다른 작성자가 새 공지를 올리면 예외가 발생했다.
- WebSocket handler가 CREATE/UPDATE/INACTIVATE/REACTIVATE/DELETE 전부에 타입과 내용을 강제해 action별 요청 형태를 수용하지 못했다.
- `RoomGrpcEndpoint.applyRoomNotice`에 예외 변환이 없어 실제 원인이 gRPC `UNKNOWN`으로만 노출됐다.

수정 내용.

- FE 라우터가 `payload.roomId ?? payload.roomNoticeView.roomId`를 사용한다.
- 새 공지와 재공지는 기존 활성 공지를 방 기준으로 내리고 적용한다.
- 수정/내림/재공지/삭제 대상의 작성자 검증은 그대로 유지한다.
- gateway는 transport 최소값인 roomId와 action만 확인하고 action별 정책 검증은 channel-engine service가 담당한다.
- gRPC endpoint가 실제 예외 메시지를 `INTERNAL` description으로 전달한다.

### 4. 알림 OFF 상태에서 채팅방 목록이 갱신되지 않던 문제

원인.

- 채팅방 목록의 마지막 메시지와 unreadCount 갱신이 `CHAT_MESSAGE_NOTIFICATION` 이벤트에 결합돼 있었다.
- 서버는 `message_notification_enabled = FALSE`인 사용자를 이 이벤트 대상에서 제외한다.
- 결과적으로 토스트를 끄면 채팅방 상태 갱신 이벤트까지 함께 사라졌다.

수정 내용.

- `CHAT_MESSAGE_NOTIFICATION`은 알림 토스트 전용으로 유지한다.
- 알림 설정과 관계없이 활성 멤버에게 전달되는 `CHAT_ROOM_UPDATED` 이벤트를 추가했다.
- 현재 해당 방을 보고 있는 사용자는 기존 `MSG_CREATED`로 상태를 갱신하고, 보고 있지 않은 사용자는 `CHAT_ROOM_UPDATED`로 목록을 갱신한다.
- 채팅 목록은 더 이상 `CHAT_MESSAGE_NOTIFICATION`을 상태 변경 근거로 사용하지 않는다.
- Kafka durable save와 메시지 DB 비동기 저장 흐름은 변경하지 않았다.

### 5. 1:1 채팅방 목록 썸네일 가이드

이번 항목은 지시대로 코드 변경하지 않았다.

권장 변경 방향.

- 현재 `domain-service RoomMapperXml.getMyAllChatRooms`는 `me.custom_room_thumbnail`을 그대로 반환한다.
- DIRECT 방에서는 이 값을 사용하지 말고 상대 `room_members.user_id`를 통해 `users.profile_img`를 조회해야 한다.
- GROUP 방은 현재처럼 `me.custom_room_thumbnail`을 유지한다.
- 권장 결과식은 `CASE WHEN r.room_type = 'DIRECT' THEN 상대 users.profile_img ELSE me.custom_room_thumbnail END AS customRoomThumbnail` 형태다.
- 상대 프로필 변경이 즉시 목록에 반영되도록 DIRECT 썸네일을 `room_members`에 복제 저장하지 않는 편이 안전하다.
- 상대가 LEFT 상태여도 기존 DIRECT 방의 상대는 동일하므로 상대 멤버 조회에서 ACTIVE만 강제하지 않는 정책 검토가 필요하다.
- 실제 적용 전 DIRECT 방 재입장/차단/탈퇴 시 썸네일 정책을 먼저 확정해야 한다.

### 6. 리액션 선택 상태 표시

원인.

- `myReactionMap`은 현재 세션에서 리액션 이벤트를 받은 뒤에만 채워졌다.
- 방 재입장이나 과거 메시지 로드 후에는 내가 이미 선택한 리액션인지 알 수 없었다.

수정 내용.

- 메시지 페이지의 기존 reaction 집계 쿼리에 `reactedByMe`를 추가했다.
- 메시지별 추가 쿼리 없이 같은 GROUP BY 결과에서 현재 사용자 선택 여부를 계산한다.
- 리액션 선택창에서 내가 선택한 이모티콘은 노란 배경, 테두리, 음영 효과로 표시한다.
- 텍스트 안내는 추가하지 않았다.

### 7. 리액션 멤버 목록 버튼 분리

수정 내용.

- 기존 이모티콘 요약 전체를 클릭해야 멤버 목록이 열리던 동작을 제거했다.
- 리액션 추가 버튼 옆에 사람 상반신 아이콘 `👤` 버튼을 추가했다.
- 이 버튼만 기존 `openReactionMemberViewer`를 호출한다.
- 이모티콘별 반응 수와 기존 멤버 상세 패널의 내용은 변경하지 않았다.

### 검증 결과

- `npm.cmd run build` 성공.
- `./gradlew.bat --offline --no-daemon --console=plain :common-contract:generateProto :common-contract:compileJava :domain-service:compileJava :websocket-gateway:compileJava :channel-engine:compileJava` 성공.
- channel-engine/domain-service의 수정된 MyBatis XML 3개 문법 검사 성공.
- 실제 DB와 WebSocket 다중 사용자 통합 테스트는 실행 중인 서비스와 로그인 세션이 필요하므로 코드 빌드 검증까지만 수행했다.
