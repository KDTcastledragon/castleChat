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

## 2026-07-10 공지·권한·배경·썸네일·unread 후속 수정

### 1. INACTIVE 공지 수정 시 현재 ACTIVE 공지가 사라지던 문제

원인.

- `ROOM_NOTICE_APPLIED` 응답의 공지가 `ACTIVE`가 아니면 공지 ID를 확인하지 않고 `currentRoomNotice`를 무조건 `null`로 바꿨다.
- 따라서 2번 INACTIVE 공지를 수정해도 별개의 3번 ACTIVE 공지가 FE에서 사라졌다.
- DB의 3번 공지는 계속 ACTIVE였으므로 DB 상태가 아니라 FE 상태 축약 로직의 문제였다.

수정 내용.

- 응답 공지가 ACTIVE이면 현재 공지로 교체한다.
- 응답 공지가 INACTIVE/DELETED이고 현재 공지와 같은 `roomNoticeId`일 때만 현재 공지를 제거한다.
- 서로 다른 INACTIVE 공지의 수정 응답은 현재 ACTIVE 공지를 그대로 보존한다.

### 2. 활성 공지를 작은 독립 창으로 변경하고 이력에 날짜 표시

문제.

- 기존 공지 바는 채팅방 상단의 문서 흐름을 차지해 메시지 영역을 밀어냈다.
- 공지 이력에는 시간만 보여 날짜가 다른 공지를 구분하기 어려웠다.

수정 내용.

- 활성 공지 UI를 채팅방 내부의 `position: absolute` floating card로 변경했다.
- 작은 카드의 `숨기기/표시하기`로 열고 닫을 수 있고, 숨겨도 서버 공지 상태는 바꾸지 않는다.
- 공지 이력 시각을 `yyyy.MM.dd HH:mm` 형식으로 표시한다.

### 3. 권한 변경 확인 후 선택 메뉴가 남던 문제

원인.

- 권한 선택 UI가 네이티브 `<details>`이고, 권한 변경 요청 후 `open` 상태를 닫는 코드가 없었다.

수정 내용.

- 확인창에서 승인하면 클릭한 버튼의 가장 가까운 `<details>`를 찾아 `open = false`로 닫는다.
- 취소하면 기존 선택 메뉴를 유지한다.

### 4. 방 배경 이미지에 투명 효과가 적용되던 문제

원인.

- 배경 이미지 위에 반투명 흰색 `linear-gradient`를 함께 합성해 원본 색이 흐려졌다.

수정 내용.

- 합성 gradient를 제거하고 사용자가 설정한 이미지 URL만 `background-image`로 적용한다.
- `cover`와 `center` 배치만 유지하며 opacity나 색상 overlay는 적용하지 않는다.

### 5. 사용자 채팅창 크기 조절 가이드

이번 항목은 지시대로 코드 변경하지 않았다.

권장 구현 방향.

- 최상위 `.chattingRoomSection`에 `resize: both`와 `overflow: hidden`을 적용한다.
- 최소 크기와 화면을 벗어나지 않는 최대 크기를 함께 제한한다.
- 현재 고정값인 `.chattingBox`와 `.inputChat`의 width/height를 부모 기준 `100%`, `flex: 1`, `min-height: 0` 구조로 바꿔야 실제 내용도 창 크기를 따라간다.
- `ResizeObserver`로 최종 width/height를 감지해 Redux의 각 chat window 상태에 저장하면 페이지 이동 후에도 열린 창의 크기를 유지할 수 있다.
- 브라우저 재접속 이후까지 유지하려면 Redux 상태를 localStorage에 동기화하거나 사용자별 방 UI 설정으로 서버에 저장한다.

### 6. DIRECT 채팅 목록 썸네일이 상대 최신 프로필을 반영하지 않던 문제

원인.

- 채팅방 내부 멤버 목록은 `users.profile_img`를 조회해 최신 이미지가 보였다.
- 채팅 목록은 `room_members.custom_room_thumbnail`에 복제된 과거 값을 읽어 두 화면의 데이터 원천이 달랐다.

수정 내용.

- `getMyAllChatRooms`에서 DIRECT 방은 상대 room member의 `users.profile_img`를 조회한다.
- GROUP 방은 방별 커스텀 썸네일 정책이므로 기존 `me.custom_room_thumbnail`을 유지한다.
- DIRECT 상대가 LEFT 상태여도 기존 방의 상대 식별이 가능하도록 썸네일 하위 조회에는 ACTIVE 조건을 강제하지 않았다.

### 7. 실시간 read 중 채팅 목록 unread가 누적되고 과거 값이 되살아나던 문제

원인.

- 방 입장 시 `visibleRooms` 화면 상태만 0으로 만들고 React Query의 `['myAllRooms']` 캐시는 바꾸지 않았다.
- ChatBox가 실시간으로 보낸 `READ_MESSAGE`와 서버의 `MSG_READ` 응답을 ChatList가 처리하지 않았다.
- read의 DB LRM 반영은 Redis dirty flush worker가 비동기로 수행하므로, flush 전 목록 재조회는 과거 DB LRM 기준 unread를 반환할 수 있다.
- 이 오래된 React Query 응답이 화면의 0을 다시 덮은 뒤 새 메시지마다 증가해 `n + 과거 unread` 형태가 됐다.

수정 내용.

- 방 입장과 본인의 `MSG_READ` 수신 시 화면 목록과 React Query 캐시의 unread를 함께 0으로 만든다.
- 방별 로컬 unread override를 유지해 dirty flush 전의 오래된 DB 스냅샷이 0을 되살리지 못하게 한다.
- `CHAT_ROOM_UPDATED`는 방을 보고 있지 않을 때만 unread를 1 증가시키고, 방 안에서 받는 `MSG_CREATED`는 마지막 메시지와 시각만 갱신한다.
- 화면 상태와 Query cache에 같은 순수 updater를 적용하되 unread 증가값은 updater 밖에서 한 번만 계산해 이중 증가를 막는다.
- read 요청에서 동기 DB 조회나 update는 추가하지 않았으며 기존 Redis + dirty flush 정책을 유지한다.

### 후속 검증 결과

- `npm.cmd run build` 성공.
- 이번에 수정한 `ChatList.jsx`의 hook dependency warning은 제거됐다.
- 기존 `AdminPage.jsx`, `ChatBox.jsx`, `JoinPage.jsx`의 선행 ESLint warning은 이번 변경 범위 밖이라 유지했다.
- `RoomMapperXml.xml` DTD 무시 XML 파싱 성공.
- `plan.md` 원칙에 따라 Gradle refresh, project clean, `gradlew.bat`은 실행하지 않았다.
- 실제 DB/WebSocket 다중 사용자 통합 검증은 실행 중인 전체 서비스와 두 로그인 세션이 필요하므로 수행하지 않았다.

## 2026-07-10 DIRECT 썸네일 실시간 반영과 헤더 현재 페이지 표시

### 1. 상대 프로필 변경이 열린 채팅방 목록에 반영되지 않던 문제

원인.

- 이전 수정은 DIRECT 목록 조회 시 `users.profile_img`를 읽도록 데이터 원천만 바로잡았다.
- 이미 상대가 채팅 목록을 열어 둔 상태에서는 `getMyAllChatRooms`를 다시 호출할 사건이 없었다.
- React Query의 `myAllRooms` cache가 기존 썸네일 URL을 계속 가지고 있어 DB 값이 바뀌어도 열린 화면은 그대로였다.
- 따라서 SQL만 바꾼 이전 수정은 새로 조회하는 경우만 해결했고 실시간 반영 경로는 해결하지 못했다.

수정 내용.

- 프로필 HTTP 저장 성공 후 FE가 `PROFILE_UPDATED` WebSocket 이벤트를 보낸다.
- gateway는 payload의 이미지 URL을 신뢰하지 않고 로그인 세션의 userId/publicId만 사용한다.
- 기존 `findOnlineFriendTargets` gRPC 흐름을 재사용해 프로필 변경자의 친구들에게만 `FRIEND_PROFILE_UPDATED`를 전송한다.
- 이벤트를 받은 상대 AppShell은 `friends`와 `myAllRooms` Query만 invalidate한다.
- 채팅 목록이 열려 있으면 즉시 재조회하고, 다른 페이지라면 다음 목록 진입 때 최신 값을 조회한다.
- 주기적 polling이나 전체 접속자 broadcast는 추가하지 않았다.

처리 흐름.

`MyPage.updateMyProfile 성공 -> emitWsProfileUpdated -> WsGateDispatcher -> WsGateConnectionHandler.handleProfileUpdated -> 기존 친구 대상 gRPC 조회 -> FRIEND_PROFILE_UPDATED -> 상대 AppShell invalidateQueries -> getMyAllChatRooms -> 최신 users.profile_img`

### 2. 헤더에서 현재 페이지를 구분할 수 없던 문제

원인.

- 헤더 버튼이 `useNavigate`만 사용하고 현재 pathname을 읽지 않았다.
- hover 순간을 제외하면 모든 버튼이 같은 스타일이었다.

수정 내용.

- `useLocation`의 `location.pathname`과 각 버튼 경로를 비교한다.
- 현재 경로의 버튼에 `active` class를 적용한다.
- active 상태는 요청대로 기존 hover와 같은 배경색과 글자색을 유지한다.

### 검증 결과

- `npm.cmd run build` 성공.
- 이번 변경으로 추가된 ESLint 오류나 경고는 없다.
- 기존 AdminPage/ChatBox/JoinPage 경고는 이번 범위 밖이라 유지했다.
- `git diff --check` 통과.
- `plan.md` 원칙에 따라 `gradlew.bat`과 Gradle refresh는 실행하지 않았다.

========================================================================================================================

## 2026-07-11 회원가입 프로필 버튼 중복과 415 multipart 오류

### 1. 회원가입 화면에 파일 관련 버튼이 두 개 보이던 문제

원인.

- 실제 파일 탐색기를 여는 `파일 선택` 버튼과 선택 파일을 취소하는 `기본 이미지` 버튼을 함께 표시하고 있었다.
- 회원가입 시 기본 이미지는 이미 서버 정책으로 결정되므로 별도 복귀 버튼이 필수 기능은 아니었다.

수정 내용.

- 화면에는 파일 탐색기를 여는 `파일 선택` 버튼 하나만 남겼다.
- 제거된 기본 이미지 버튼의 CSS도 함께 삭제했다.

### 2. 기본 이미지 회원가입이 415 Unsupported Media Type으로 끝나던 문제

원인.

- 이미지가 없어도 프론트가 항상 `FormData`를 만들어 multipart 요청을 보내고 있었다.
- 실행 중인 8080 서버는 해당 요청을 `RequestResponseBodyMethodProcessor`로 처리하려 했다. 이는 현재 소스의 multipart handler가 아니라 이전 `@RequestBody` JSON controller가 실행 중이라는 증거다.
- `DefaultHandlerExceptionResolver`가 controller의 지원 media type과 실제 요청 Content-Type 불일치를 `HttpMediaTypeNotSupportedException`으로 처리해 415를 반환했다.

수정 내용.

- 이미지 미선택 가입은 JSON으로 보내고 서버가 `/images/mococo_question.png`를 저장한다.
- 실제 이미지가 선택된 가입만 multipart/form-data로 보낸다.
- 서버의 같은 `/user/join` URL에 JSON handler와 multipart handler를 `consumes` 기준으로 분리했다.
- 현재 실행 중인 `domain-service`에는 변경 소스가 반영되지 않았으므로 컴파일 후 프로세스 재시작이 필요하다.

### 검증 결과

- 수정 전 실행 중인 8080 endpoint에 multipart 요청을 보내 415와 `RequestResponseBodyMethodProcessor` 호출 경로를 재현했다.
- 프론트 빌드와 diff 검증 결과는 아래 작업 기록에 반영한다.

========================================================================================================================

## 2026-07-11 회원가입 경로 차단과 그룹 draft 중복 생성 수정

### 1. 로그인 화면에서 회원가입을 눌러도 회원가입 페이지가 열리지 않던 문제

원인.

- `/join` Route와 회원가입 버튼의 `navigator('/join')`는 정상적으로 존재했다.
- AppShell의 로그인 확인 effect가 비로그인 상태의 모든 URL을 `/login`으로 강제 이동시켰다.
- 따라서 `/join`으로 이동한 직후 같은 effect가 다시 `/login`으로 되돌렸다.

수정 내용.

- `/login`, `/join`을 비로그인 접근이 가능한 공개 경로로 명시했다.
- 비로그인 상태이면서 공개 경로가 아닌 경우에만 `/login`으로 이동한다.
- 공개 경로에서는 WebSocket을 연결하지 않고 해당 화면을 그대로 렌더링한다.

### 2. 그룹 draft에서 첫 메시지 Enter 연타 시 방이 여러 개 생성되던 문제

원인.

- draft 첫 메시지는 `START_GROUP_CHAT` 응답을 기다리는 비동기 요청이다.
- 기존 차단 조건은 파일 업로드용 `isUploadingFiles`뿐이어서 텍스트 기반 START 요청에는 적용되지 않았다.
- 첫 요청의 응답이 오기 전에 Enter가 다시 입력되면 `sendChatMessage()`가 재진입해 START 요청이 병렬 전송됐다.
- React state만 추가할 경우 state 반영 전 같은 event loop에서 들어오는 연속 keydown을 완전히 막을 수 없다.

수정 내용.

- draft START 요청 전용 `startChatRequestLockRef`를 추가했다.
- 첫 요청을 보내기 전에 ref를 동기적으로 잠그므로 같은 렌더 사이클에서 Enter가 반복돼도 후속 요청은 즉시 종료된다.
- 응답 성공 또는 예외가 끝나는 `finally`에서만 잠금을 해제한다.
- `isStartingChat` state는 전송 버튼 비활성화와 `전송 중` 표시만 담당한다.
- 기존 방의 일반 `SEND_MESSAGE` 연속 전송 흐름은 변경하지 않았다.

### 검증 결과

- `npm.cmd run build` 성공.
- 기존 AdminPage, ChatBox, JoinPage의 선행 ESLint warning만 남았다.
- `git diff --check` 통과.
- `plan.md` 원칙에 따라 Gradle과 project refresh는 실행하지 않았다.

========================================================================================================================

## 2026-07-11 활성 공지 폭과 START 첫 메시지 송신자 렌더 누락 수정

### 1. 활성 공지 폭이 좁고 숨김/표시 toggle 시 버튼 위치가 이동하던 문제

원인.

- 펼친 공지는 `width: min(360px, ...)`, 가운데 정렬 transform을 사용했다.
- 접힌 공지는 가운데 좌표를 제거하고 `right: 12px`, `width: auto`로 별도 배치해 toggle 시 버튼 위치가 달라졌다.

수정 내용.

- 펼침과 접힘 모두 `left: 8px`, `right: 8px`, `width: auto`를 사용해 채팅창 가로 폭을 채운다.
- 접힘 상태에서도 동일한 bar 영역과 오른쪽 정렬을 유지해 toggle 버튼 좌표가 움직이지 않는다.
- 접힘 상태의 빈 bar는 배경, border, shadow를 투명 처리한다.

### 2. START 첫 메시지가 DB에는 저장되지만 송신자 ChatBox에서 보이지 않던 문제

원인.

- START 응답의 `StartChatResponseDTO`에는 `enterRoomInfo`와 `firstChatMessage`가 모두 포함돼 있었다.
- FE는 draft 창을 닫은 뒤 `enterRoomInfo`만 `openRoom()`에 전달하고 `firstChatMessage`를 버렸다.
- gateway가 START 응답 직후 `MSG_CREATED`를 broadcast해도 새 room handler가 등록되기 전이면 송신자는 이벤트를 놓칠 수 있었다.
- 새 ChatBox가 즉시 HTTP 메시지 조회를 시작하지만 Kafka worker의 DB insert가 아직 끝나지 않았으면 빈 목록을 반환했다.
- HTTP 응답이 늦게 도착하면 그 사이 받은 WS 메시지까지 `setPrevChattings(messages)`로 덮어쓸 수 있었다.

수정 내용.

- `openRoom(roomInfo, initialMessages)` 형태로 START 응답의 첫 메시지를 Redux 채팅창 상태에 함께 전달한다.
- AppShell이 `initialMessages`를 새 ChatBox에 전달하고 ChatBox는 첫 렌더부터 해당 메시지를 표시한다.
- HTTP 로드 결과는 기존 메모리 메시지를 지우지 않고 `messageId` 기준으로 병합한다.
- START broadcast가 새 handler 등록 후 도착해도 이미 같은 `messageId`가 있으면 중복 추가하지 않는다.
- Kafka durable save 후 응답, 비동기 DB insert, 기존 REST/WS/backend 흐름은 변경하지 않았다.

### 3. 첫 메시지에서만 체감 지연이 컸던 이유와 개선

- 첫 메시지는 일반 SEND와 달리 방 생성, 멤버 생성, cache 초기화, Kafka durable save, 입장 정보 조립이 포함돼 본질적으로 작업량이 더 많다.
- 기존에는 이 작업을 기다린 뒤 새 ChatBox가 다시 HTTP DB 조회까지 수행해야 첫 메시지가 보였다.
- 이제 START 응답에 이미 포함된 첫 메시지를 즉시 렌더 source로 사용하므로 DB worker 반영과 재조회 완료를 기다리지 않고 표시된다.

### 검증 결과

- `npm.cmd run build` 성공.
- 기존 AdminPage, ChatBox, JoinPage의 선행 ESLint warning만 남았다.
- `git diff --check` 통과.
- `plan.md` 원칙에 따라 Gradle과 project refresh는 실행하지 않았다.
- 두 로그인 세션을 사용한 런타임 WebSocket 검증은 수행하지 않았다.

## 2026-07-11 내정보·회원가입 프로필·비밀번호 모달·채팅 목록 개선

### 1. 내 프로필을 기본 이미지로 되돌릴 수 없던 문제

원인.

- 내정보 화면에는 새 파일 선택만 있었고 기본 이미지 경로를 선택 상태로 만드는 동작이 없었다.

수정 내용.

- 기본 이미지 버튼을 추가했다.
- 선택하면 미리보기와 선택 파일명이 `/images/mococo_question.png`, `mococo_question.png`로 바뀐다.
- 프로필 이미지 변경 버튼을 눌러야 DB에 저장되며 선택만으로 서버 상태를 바꾸지 않는다.

### 2. 프로필 이미지 URL 전체가 노출되던 문제

원인.

- `me.profileImg`를 화면에 그대로 출력해 `/uploads/.../UUID.png` 전체 경로가 보였다.

수정 내용.

- query string과 경로를 제거하고 마지막 파일명만 표시하는 `getProfileImageFileName`을 추가했다.
- 기본 정보와 파일 선택 왼쪽 칸 모두 파일명만 표시한다.
- 파일 선택 칸은 `overflow: hidden`, `white-space: nowrap`, `text-overflow: ellipsis`를 적용하고 전체 이름은 title로 확인할 수 있다.

### 3. 내정보의 publicId 노출 제거

- publicId 행을 제거했다.
- loginId로 대체하지 않았다. 로그인 식별자는 본인 화면에서도 반복 노출할 UX 이득이 없고 계정 식별 정보 노출만 늘리기 때문이다.
- 내정보에는 닉네임, 친구코드, 프로필 이미지 파일명만 표시한다.

### 4. 내정보 제목 설명 제거

- `프로필과 기본 정보를 여기서 관리합니다.` 문구와 사용하지 않게 된 CSS를 제거했다.

### 5. 닉네임과 프로필 이미지 저장 분리

원인.

- 기존 `updateMyProfile`은 닉네임과 이미지가 항상 함께 update돼 한 필드만 수정해도 다른 필드까지 DB update 대상이 됐다.

수정 내용.

- FE 버튼을 `닉네임 변경`, `프로필 이미지 변경`으로 분리했다.
- HTTP endpoint를 `/user/updateMyNickname`, `/user/updateMyProfileImage`로 분리했다.
- MyBatis update도 `updateMyNickname`, `updateMyProfileImage`로 분리했다.
- 각 성공 응답은 기존 세션의 변경하지 않은 필드를 보존하고 `me` Query cache를 갱신한다.
- 닉네임 또는 이미지 변경 후 기존 `PROFILE_UPDATED` WebSocket 알림을 보내 친구 화면도 최신 정보를 재조회한다.

### 6. 헤더 프로필 이미지에서 내정보 이동

- 헤더 이미지를 접근 가능한 button으로 감쌌다.
- 클릭하면 `/myPage`로 이동한다.
- hover 시 원형 outline을 표시해 클릭 가능한 영역임을 보여준다.

### 7. 회원가입 프로필 이미지와 기본 이미지 저장

원인.

- 회원가입 요청은 JSON만 받았고 `users.profile_img`를 INSERT하지 않았다.
- 이미지 미선택 사용자는 DB 값이 NULL이라 화면마다 서로 다른 fallback 처리가 필요했다.

수정 내용.

- 회원가입 UI에 이미지 파일 선택과 기본 이미지 복귀 버튼을 추가했다.
- 가입 요청을 multipart FormData로 변경해 loginId, password, nickname, 선택 이미지를 한 요청으로 보낸다.
- 가입 전 별도 공개 업로드 endpoint는 만들지 않았다. 이미지 저장과 가입 요청을 분리하면 가입하지 않은 사용자의 고아 파일이 더 쉽게 쌓이기 때문이다.
- 선택 이미지는 기존 공통 이미지 저장 로직을 재사용하며 프로필 이미지는 최대 10MB로 제한한다.
- 미선택 시 `/images/mococo_question.png`를 `users.profile_img`에 명시적으로 INSERT한다.
- 기존 NULL/빈 문자열 사용자는 DIRECT 목록 및 채팅방 멤버 조회에서 SQL `COALESCE`로 기본 이미지를 반환한다.

### 8. 비밀번호 변경 입력이 본문에 펼쳐지던 문제

수정 내용.

- 비밀번호 변경을 fixed modal overlay로 변경했다.
- overlay는 아주 옅은 회색 배경으로 전체 화면을 덮어 뒤쪽 UI 클릭을 차단한다.
- 현재 비밀번호, 새 비밀번호, 새 비밀번호 확인 입력을 제공한다.
- 새 비밀번호 두 값이 다르면 HTTP 요청 전에 차단한다.
- 닫기와 취소 시 입력값을 모두 초기화한다.

### 9. 채팅 목록 가로 스크롤과 과도한 컨테이너 폭

원인.

- `.chatList`에 세로 overflow가 생기면 scrollbar 폭만큼 내부 가용 폭이 줄었지만 자식은 고정 680px과 margin을 유지했다.
- 바깥 컨테이너는 실제 목록 700px보다 훨씬 큰 1800px였다.

수정 내용.

- 바깥 컨테이너를 목록과 여백에 맞는 724px로 조정했다.
- 목록에 `box-sizing: border-box`, 내부 padding, `overflow-x: hidden`을 적용했다.
- title과 row는 고정 폭 대신 부모의 `width: 100%`를 사용한다.

### 검증 결과

- `npm.cmd run build` 성공.
- 수정한 MyBatis XML 4개 DTD 무시 파싱 성공.
- `git diff --check` 통과.
- 기존 AdminPage, ChatBox, JoinPage의 선행 ESLint warning은 이번 범위 밖이라 유지했다.
- `plan.md` 원칙에 따라 `gradlew.bat`과 Gradle refresh는 실행하지 않았다.
- Java 변경은 사용자가 Gradle을 실행하기 전까지 컴파일 검증되지 않은 상태다.

========================================================================================================================

## 2026-07-11 전체 테마 교체와 AI 메시지 추천 예외 수정

### 1. 화면마다 노랑·형광색·원색이 섞여 포트폴리오 인상이 일관되지 않던 문제

원인.

- 헤더는 형광 연두, 청록, 보라를 직접 RGB 값으로 사용하고 있었다.
- 친구 목록, 채팅 목록, ChatBox의 선택 상태와 메시지 bubble은 카카오톡과 동일한 노랑 계열을 각각 직접 선언하고 있었다.
- 화면별 공통 색상 기준이 없어 같은 의미의 버튼과 선택 상태도 서로 다른 색을 사용했다.

수정 내용.

- `index.css`에 딥 네이비, 슬레이트, 틸 기반의 공통 CSS 변수를 정의했다.
- 주 배경은 `#f4f7fb`, 표면은 흰색, 상단 강조는 `#0f172a`, 핵심 액션은 `#0f766e`를 사용한다.
- Header, 친구 목록, 채팅 목록, 로그인, 회원가입, 내정보, 설정, 관리자 화면의 기존 class에 동일 토큰을 적용했다.
- ChatBox의 내 메시지는 짙은 틸과 흰 글자, 상대 메시지는 밝은 슬레이트와 짙은 글자로 구분했다.
- 공지, 리액션, AI 추천, 초대, 멤버 설정의 선택 상태는 옅은 틸 배경으로 통일했다.
- 위험 액션의 빨강과 unread의 주황은 의미 전달을 위해 유지했다.
- JSX 구조, API 호출, 상태, 이벤트 handler, 렌더링 데이터는 변경하지 않았다. 업로드 진행 원형의 인라인 색상 값만 CSS 변수로 교체했다.

### 2. AI에게 메시지 추천받기 요청이 exception으로 끝나던 문제

원인.

- AI 서비스가 사용하던 `gemini-2.0-flash` 모델은 2026-06-01 종료되어 기존 generateContent 요청이 더 이상 정상 처리되지 않는다.
- 기존 예외는 공급자 HTTP status와 response body를 남기지 않아 모델 종료, API key 누락, quota 초과를 로그에서 구분하기 어려웠다.
- 추천 endpoint가 로그인 세션만 확인하고 요청자가 해당 방의 ACTIVE 멤버인지 확인하지 않았다.

수정 내용.

- 설정 모델과 Java 기본 모델을 공식 대체 모델인 `gemini-3.5-flash`로 변경했다.
- API key 미설정은 외부 호출 전에 명시적으로 차단한다.
- Gemini HTTP 실패는 model, HTTP status, response body를 서버 로그에 남기고 FE에는 status가 포함된 오류 메시지를 반환한다.
- 최근 대화를 조회하기 전에 요청자가 해당 방의 ACTIVE 멤버인지 확인한다.
- 호출 방식은 기존 저빈도 HTTP 요청/응답 구조를 유지했다.

### 3. 모니터별 화면 크기 차이 대응 가이드

이번 작업에서는 사용자의 지시에 따라 반응형 레이아웃을 직접 적용하지 않았다. 후속 작업은 다음 순서가 적합하다.

1. Header 1500px, FriendList 1280px, ChatList 724px처럼 화면 컨테이너에 박힌 고정 폭을 먼저 목록화한다.
2. 최상위 화면 폭을 `width: min(calc(100% - 32px), 1440px)` 형태의 max-width 컨테이너로 변경한다.
3. 친구 화면의 3열은 CSS Grid `repeat(3, minmax(0, 1fr))`로 유지하고 1024px 이하에서 2열, 768px 이하에서 1열로 전환한다.
4. 채팅창은 기능상 최소 폭을 보장하기 위해 `width: clamp(360px, 32vw, 520px)`처럼 제한하고 viewport 밖으로 나간 좌표를 resize 시 보정한다.
5. 레이아웃 분기는 JavaScript의 `window.innerWidth`보다 CSS media query를 사용한다. 채팅창 위치처럼 상태가 필요한 값만 ResizeObserver와 Redux에서 처리한다.
6. 1366x768, 1440x900, 1920x1080, 2560x1440과 브라우저 확대 100%, 125%, 150% 조합을 검증 기준으로 둔다.
7. 각 기준에서 가로 스크롤, 겹침, 잘린 버튼, modal viewport 이탈을 확인하는 시각 회귀 체크리스트를 만든다.

이 방식은 특정 면접관 모니터 해상도를 추측하지 않고 가용 viewport를 기준으로 자연스럽게 재배치하므로 유지보수와 실무 대응에 유리하다.

### 검증 결과

- `npm.cmd run build` 성공.
- 기존 AdminPage, ChatBox, JoinPage의 선행 ESLint warning은 유지됐다.
- `AiAssistMapperXml.xml` DTD 무시 XML 파싱 성공.
- `git diff --check` 통과.
- 로컬 3000 포트의 개발 서버가 실행 중이지 않아 실제 브라우저 시각 검증은 수행하지 못했다.
- `plan.md` 원칙에 따라 `gradlew.bat`과 Gradle refresh는 실행하지 않았다.

========================================================================================================================
