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
