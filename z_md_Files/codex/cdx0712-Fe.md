## 2026-07-11 Compact Layout Adjustment

- Header height.
  - `Header.css`의 `.HeaderContainer` 높이를 `100px`에서 `85px`로 줄였다.
  - 사용자 요청대로 현재 높이에서 15%만 줄이는 기준을 적용했다.
  - 프로필 이미지, 로그아웃 버튼, header button font도 줄어든 header에 맞춰 조금 낮췄다.

- Friend main page boxes.
  - `Friends.css`의 `.FriendListContainer`, `.friendPanel`, `.friendScrollBox`, `.searchResultBox` 높이를 줄였다.
  - 목적은 1080p보다 낮은 실제 브라우저 가시 영역에서도 우측 body scrollbar가 덜 생기게 하는 것이다.
  - 3열 구조와 친구 선택, 단톡 생성, 친구 검색 흐름은 바꾸지 않았다.

- Chat room list box.
  - `ChatList.css`의 `.ChatListContainer` 폭을 `724px`에서 `434px`로 줄였다.
  - 기존 폭의 약 60%만 남기는 요청을 적용했다.
  - `margin: 8px 0 0 8px`로 바꿔 왼쪽에 거의 붙게 했다.
  - 내부 `.chatList`, row button, thumbnail, room name, last message width도 같이 줄여 가로 스크롤이 생기지 않게 했다.

- ChatBox height.
  - `ChatBox.css`의 `.chattingRoomSection`에 `max-height: calc(100vh - 24px)`를 추가했다.
  - `.chattingBox` 메시지 영역을 `500px`에서 viewport 기반 `min(390px, calc(100vh - 236px))`로 줄였다.
  - 입력창은 `100px`에서 `82px`로 줄여 채팅창이 header와 화면 하단을 덜 침범하게 했다.

- Profile image file name comment.
  - 내정보에서 원래 업로드한 `해병`, `전순사진` 같은 이름 대신 임의 문자열이 보이는 이유는 서버 저장 파일명을 충돌 방지용 uuid/hash 값으로 바꾸기 때문이다.
  - 원본 파일명을 그대로 보여주려면 서버가 업로드 시 `original_file_name`을 따로 저장하거나, profile image response에 원본 파일명을 별도 필드로 내려줘야 한다.
  - 이번 요청에서는 직접 해결하지 말라는 지시가 있었으므로 코드 변경은 하지 않았다.

## 2026-07-12 Discord Single View Regression Fixes

### 1. 입력 영역 가로 폭 정합

- 원인은 `.inputChat` 내부 고정 폭 합이 부모 폭보다 컸던 것이다. 첨부 버튼 `70px`, textarea `342px`, 전송 버튼 `76px`의 합이 `488px`라 기존 `460px` 부모부터 이미 넘치고 있었다.
- 사용자가 조정한 첨부 버튼 `70px`은 유지했다.
- 전송 버튼은 요청대로 기존 `76px`에서 `86px`로 10px 늘렸다.
- textarea는 남은 공간을 `flex: 1`로 받게 바꿔 세 요소의 합이 항상 채팅방 가로 폭과 정확히 일치하게 했다.

### 2. 채팅방 상단 bar 46px 고정

- 원인은 column flex 안의 `.chatListTitle`이 기본 `flex-shrink: 1` 상태라 메시지와 하단 composer가 공간을 차지할수록 제목 bar가 압축된 것이다.
- `height`, `min-height`, `flex-basis`를 모두 `46px`로 고정했다.
- 새 높이에 맞춰 활성 공지, 방 메뉴, 공지 이력 panel, 프로필 popup의 시작 위치도 함께 조정했다.

### 3. 리액션 직후 최하단 강제 이동 제거

- 원인은 `prevChattings` 배열이 조금이라도 바뀔 때마다 `scrollIntoView`를 실행하던 effect다.
- 리액션, 읽음 수, 삭제 상태 갱신도 새 배열을 만들기 때문에 모두 새 메시지로 오인되어 최하단으로 이동했다.
- 자동 스크롤 조건을 `마지막 messageId 또는 메시지 개수가 실제로 변경됨 + 현재 사용자가 하단을 보고 있음`으로 좁혔다.
- 리액션과 읽음처럼 기존 메시지 객체만 변경되는 경우 현재 scrollTop을 유지한다.

### 4. 리액션 이후 상대 프로필 이미지 위치 고정

- 원인은 `.chatRow`의 `align-items: flex-end` 때문에 리액션 bar가 messageContent 아래에 추가될 때 프로필 이미지도 행의 새 바닥으로 내려간 것이다.
- 상대 프로필 이미지만 `align-self: flex-start`로 분리하고 닉네임 높이만큼 상단 여백을 줬다.
- 시간과 unread 표시의 기존 하단 정렬은 건드리지 않았다.

### 5. 답장 대상 메시지로 이동

- 각 일반 메시지 row에 `data-message-id`를 부여했다.
- 답장 미리보기를 마우스 또는 Enter/Space로 선택하면 현재 채팅 scroll container 안에서 원본 메시지를 찾아 중앙으로 부드럽게 이동한다.
- 이동한 메시지는 1.4초 동안 테두리 강조가 표시된다.
- 현재 페이지에 로드된 원본 메시지를 대상으로 하며, 아직 pagination으로 로드하지 않은 과거 메시지는 후속 서버 snapshot 또는 추가 paging 정책이 필요하다.

### 6. 방 입장 시 중간 위치가 보이던 문제 수정

- 기존 smooth `scrollIntoView`가 최상단 구간을 지나가는 동안 과거 메시지 추가 로딩을 유발하고, 이미지와 영상 높이가 늦게 확정되면서 최종 위치가 중간으로 밀렸다.
- 내부 scroll container의 `scrollTop`을 직접 마지막 높이로 맞춰 최상단 threshold를 통과하지 않게 했다.
- 이미지 `load`와 영상 `loadedMetadata` 이후에도 사용자가 하단을 보던 상태에서만 다시 최하단을 맞춘다.
- 사용자가 과거 메시지를 읽으려고 위로 올린 상태에서는 새 메시지나 미디어 로딩으로 강제 이동하지 않는다.

### 7. 다른 방 메시지 unread가 목록에 쌓이지 않던 문제 수정

- 싱글뷰 전환은 Redux의 활성 ChatBox만 교체하고 이전 방에 `EXIT_ROOM`을 보내지 않았다.
- gateway는 사용자가 이전 방을 계속 보고 있다고 판단해 `CHAT_ROOM_UPDATED` 대상에서 제외했다.
- 그래서 `MSG_CREATED`로 마지막 메시지만 갱신되고 unread 증가 이벤트는 받지 못했다.
- 새 방 ENTER 성공 후 이전 roomId에 EXIT를 보내도록 `useChatRoomActions`를 수정했다.
- `/chatList`를 떠날 때 현재 방 EXIT를 보내고, 같은 방을 유지한 채 돌아오면 ENTER를 다시 보내 gateway viewing registry를 복구한다.
- 채팅방 입장 실패 전에는 목록 unread를 먼저 0으로 만들지 않도록 초기화 시점도 ENTER 성공 이후로 옮겼다.

### 8. exit 후 재입장 시 room feed 복원

- 기존 feed는 WebSocket DTO를 받아 `ChatBox` 메모리에만 `SYSTEM` 항목으로 추가했으며 DB insert가 전혀 없었다.
- room action 트랜잭션 안에서 Snowflake messageId를 발급하고 `chat_messages`에 `message_type = 'SYSTEM'`으로 동기 저장하도록 연결했다.
- 적용 대상은 LEFT, INVITE, KICK, BAN, ROLE_CHANGED와 공지 CREATE/UPDATE/INACTIVATE/REACTIVATE/DELETE다.
- 기존 `/room/loadMessagesInRoom` pagination이 SYSTEM row도 함께 반환하므로 별도 proto와 별도 조회 API 없이 재입장 시 같은 타임라인에 복원된다.
- SYSTEM feed는 채팅 목록 unread 계산과 메시지 unread 계산에서 제외했다.
- DB enum 변경 SQL은 `z_md_Files/cdx0712-feed-schema.sql`에 작성했다. 이 SQL을 DB에 적용한 이후 발생하는 feed부터 저장된다.
- 과거 feed는 애초에 저장된 원본 row가 없으므로 소급 복원할 수 없다.

### 9. 우클릭 메뉴와 활성 공지 위치 수정

- 도킹 모드가 `.chattingRoomSection`을 `position: static`으로 바꾸면서 absolute 자식의 좌표 기준 부모가 사라졌다.
- JavaScript는 채팅방 rect 기준 좌표를 계산하지만 CSS는 viewport 기준으로 그려 서로 다른 좌표계를 사용한 것이 직접 원인이다.
- 도킹 상태를 `position: relative`로 바꿔 우클릭 메뉴, 리액션 picker, 활성 공지, 공지 panel을 현재 채팅방 내부 좌표에 고정했다.

### 10. 채팅방 메뉴 시작 위치 수정

- split layout에 채팅 목록 폭 `380px`과 panel gap `8px`을 CSS 변수로 정의했다.
- 도킹된 room menu는 현재 채팅 pane에서 두 값을 합친 만큼 왼쪽으로 이동해 채팅방 목록의 왼쪽 경계에서 열린다.
- 닫힌 상태에서는 목록 바깥으로 숨고, 열린 상태에서는 채팅방 목록 영역을 덮는 drawer 방식이다.

### 검증 결과

- `npm.cmd run build` 성공. 기존 ESLint warning만 남았다.
- channel-engine `RoomMapperXml.xml` 파싱 성공.
- domain-service `RoomMapperXml.xml` 파싱 성공.
- 수정 대상 `git diff --check` 성공. CRLF 변환 warning만 출력됐다.
- `plan.md` 원칙에 따라 Gradle, project refresh, DB ALTER 실행은 하지 않았다.
- 실제 브라우저 다중 사용자 시나리오와 DB SYSTEM enum 적용 후 재입장 검증은 실행 환경에서 확인해야 한다.
