# Current Task Context Notes

## 범위

- 첨부 지시사항 1~4, 6~7은 직접 구현한다.
- 지시사항 5의 1:1 채팅 목록 썸네일은 코드를 변경하지 않고 가이드만 제공한다.
- 기존 `ChatBox.jsx`의 사용자 변경을 보존하고 현재 구현 위에 필요한 부분만 추가한다.

## 우선 원칙 검증

- 공지 이력은 저빈도 조회이므로 HTTP 기반 20개 커서 조회가 적합하다.
- 공지 적용은 권한 실패를 즉시 알려야 하므로 기존 동기 WebSocket/gRPC 경로를 유지한다.
- 알림 표시 설정은 토스트 전달만 제어해야 하며 메시지와 채팅 목록 상태 갱신을 제어하면 안 된다.
- 리액션 기존 상세 데이터와 멤버 목록 조회 기능은 유지하고 UI 진입점만 변경한다.

## 작업 중 결정

- 공지 숨김 상태는 사용자별 로컬 UI 상태로 다룬다. 공지 자체의 ACTIVE/INACTIVE 서버 상태와 분리한다.
- 삭제된 공지는 이력에서는 보존하고 내용 대신 `삭제된 공지사항입니다.`를 표시한다.
- 공지 이력은 `GET /room/{roomId}/notices`에서 `beforeRoomNoticeId` 커서와 20개 제한으로 조회한다.
- 새 공지와 재공지는 기존 활성 공지를 교체할 수 있으며, 수정/내림/재공지/삭제 대상 자체는 작성자만 조작한다.
- `CHAT_MESSAGE_NOTIFICATION`은 토스트 전용으로 유지하고 채팅 목록 상태는 `CHAT_ROOM_UPDATED`로 별도 전파한다.
- `CHAT_ROOM_UPDATED`는 알림 설정과 관계없는 전체 활성 멤버 목록을 기반으로 하되 현재 방을 보는 유저는 제외한다. 현재 방을 보는 유저는 기존 `MSG_CREATED`를 받는다.
- 리액션 선택 여부는 메시지 페이지 집계 쿼리의 `reactedByMe`로 함께 내려 별도 메시지별 조회를 만들지 않는다.
- 1:1 채팅 목록 썸네일은 이번 작업에서 코드 변경하지 않았다.

## 확인한 원인

- `ROOM_NOTICE_APPLIED`의 roomId가 `payload.roomNoticeView.roomId`에 있었지만 FE 라우터는 `payload.roomId`만 읽고 있었다.
- 새 공지 등록 시 기존 활성 공지의 작성자 권한까지 검사해 다른 사용자가 새 공지를 올릴 수 없었다.
- 채팅 목록 갱신이 알림 OFF 시 전송되지 않는 `CHAT_MESSAGE_NOTIFICATION`에 결합돼 있었다.
- 내 리액션 상태는 실시간 클릭 이후 메모리에만 기록돼 재입장 시 선택 상태를 복구할 수 없었다.

## 검증

- `npm.cmd run build` 성공. 기존 ESLint warning만 남았다.
- Gradle proto 생성 및 common-contract/domain-service/websocket-gateway/channel-engine 컴파일 성공.
- 수정한 MyBatis XML 3개를 XmlReader로 파싱해 모두 성공했다.

## 2026-07-10 Follow-up Findings

- INACTIVE 공지 UPDATE 응답이 오면 현재 활성 공지 ID와 무관하게 `currentRoomNotice`를 null로 만들고 있었다.
- 활성 공지 표시는 문서 흐름을 차지하는 상단 bar가 아니라 채팅 영역 위에 겹치는 작은 floating card로 변경한다.
- DIRECT 목록 썸네일은 `room_members.custom_room_thumbnail` 복제값 때문에 상대 프로필 변경을 따라가지 못한다. 목록 조회 시 상대 `users.profile_img`를 사용한다.
- unread 0 처리는 `visibleRooms`만 바꾸고 React Query `myAllRooms` 캐시와 `MSG_READ`를 반영하지 않아, dirty flush 전 DB 스냅샷이 과거 unread를 다시 덮고 있었다.
- 채팅방 크기 조절은 이번 범위에서 구현하지 않고 CSS/Redux 설계 가이드만 제공한다.

## 2026-07-10 Follow-up Result

- 공지 실시간 reducer는 응답 notice ID와 현재 ACTIVE notice ID를 비교하도록 변경했다.
- 활성 공지는 레이아웃을 밀지 않는 floating card이며 로컬 숨김/표시 상태를 유지한다.
- DIRECT 목록 썸네일은 상대 `users.profile_img`, GROUP 목록 썸네일은 기존 custom 값을 사용한다.
- 본인의 `MSG_READ`와 방 입장은 화면 목록과 React Query cache를 동시에 0으로 맞춘다.
- Redis dirty flush 전 DB 스냅샷이 과거 unread를 반환해도 방별 로컬 override가 최신 UI 값을 보존한다.
- unread 증가값은 상태 updater 밖에서 한 번만 계산해 화면 목록과 Query cache 이중 적용에 따른 2회 증가를 막았다.
- Gradle과 project refresh는 `plan.md` 원칙에 따라 실행하지 않았다.
- `npm.cmd run build`는 성공했고 이번 ChatList 변경에서 발생했던 hook dependency warning은 제거했다.
- 기존 AdminPage/ChatBox/JoinPage ESLint warning은 이번 범위 밖이라 수정하지 않았다.
- `RoomMapperXml.xml`은 DTD 무시 XmlReader 파싱에 성공했다.

## 2026-07-10 Direct Thumbnail And Header Route Findings

- 이전 SQL 수정은 `getMyAllChatRooms`가 다시 호출될 때 최신 `users.profile_img`를 반환하는 것까지만 해결했다.
- 이미 채팅 목록을 열어 둔 상대 브라우저에는 프로필 변경 사실을 전달하는 이벤트가 없어 React Query cache의 과거 썸네일이 계속 보인다.
- 주기적 polling은 목록 사용자 수만큼 불필요한 DB 조회를 만들기 때문에 prpr 1에 맞지 않는다.
- 프로필 변경은 저빈도이므로 WebSocket으로 변경 사실을 전달하고, 해당 DIRECT 상대가 있는 ChatList만 `myAllRooms`를 invalidate하는 방식으로 처리한다.
- Header는 `useLocation`으로 현재 pathname을 확인해 기존 hover 색상과 같은 active class를 적용한다.

## 2026-07-10 Direct Thumbnail And Header Route Result

- `PROFILE_UPDATED` 요청은 프로필 저장 성공 후에만 FE가 전송한다.
- gateway는 프로필 이미지 payload를 받지 않고 인증된 세션의 userId/publicId로 친구 대상만 조회한다.
- 상대 클라이언트는 `FRIEND_PROFILE_UPDATED` 수신 시 `friends`와 `myAllRooms` cache를 invalidate한다.
- 기존 DIRECT 썸네일 SQL과 결합되어 재조회 결과는 상대 `users.profile_img`를 사용한다.
- 전체 사용자 broadcast와 polling은 사용하지 않았다.
- Header는 pathname이 정확히 일치하는 버튼에 hover와 같은 active 효과를 적용한다.

## 2026-07-11 My Page And Join Profile Decisions

- publicId는 내정보 화면에서 제거하고 loginId로 대체하지 않는다. 로그인 식별자는 사용자에게 반복 노출할 UX 이득이 없고 민감한 계정 식별 정보만 늘린다.
- 기본 프로필 이미지 경로는 `/images/mococo_question.png`로 통일하고 회원가입 시 DB `users.profile_img`에도 실제 저장한다.
- 닉네임과 프로필 이미지는 서로 다른 버튼과 HTTP endpoint로 분리해 한 기능 저장이 다른 필드를 불필요하게 update하지 않도록 한다.
- 가입 전에는 기존 인증 필요 `/chat/image`를 사용할 수 없으므로 가입 프로필 전용 이미지 업로드 endpoint가 필요하다.
- 비밀번호 변경은 modal overlay로 분리하고 현재 비밀번호, 새 비밀번호, 새 비밀번호 확인을 FE에서 먼저 검증한다.
- 채팅 목록은 내부 고정 폭과 여백의 합이 부모 폭을 넘지 않도록 `box-sizing`과 `width: calc(...)`를 사용하고 `overflow-x: hidden`을 적용한다.

## 2026-07-11 My Page And Join Profile Result

- 회원가입은 JSON에서 multipart FormData 요청으로 변경했다.
- 가입 이미지 미선택 시 `/images/mococo_question.png`가 DB에 저장된다.
- 기존 NULL/빈 이미지 사용자는 room/chat mapper에서 기본 이미지로 보정한다.
- 닉네임과 프로필 이미지 변경은 FE mutation, controller endpoint, mapper update까지 분리했다.
- 내정보에서는 URL과 publicId를 표시하지 않는다.
- 비밀번호 확인은 FE UX 검증이며 서버에는 기존처럼 현재 비밀번호와 새 비밀번호만 보낸다.
- 채팅 목록 컨테이너는 724px, 내부 목록은 700px로 맞추고 가로 overflow를 숨긴다.
- 프론트 build와 XML 파싱은 성공했으며 Gradle은 실행하지 않았다.

## 2026-07-11 Professional Theme And AI Recommend Decisions

- 포트폴리오 테마는 딥 네이비 `#0f172a`, 슬레이트 `#334155`, 틸 `#0f766e`, 소프트 배경 `#f4f7fb` 계열로 정한다.
- 카카오톡식 배치와 기능 구조는 유지하고 CSS 색상·테두리·그림자·여백만 변경한다.
- 노란 메시지 bubble은 틸 계열로 교체하고 상대 bubble은 밝은 슬레이트로 유지해 발신 방향 가시성을 보존한다.
- 반응형 레이아웃은 이번 작업에서 직접 구현하지 않는다. 고정 px 제거, container max-width, CSS grid/flex breakpoint, chat window viewport clamp 순서만 문서로 제시한다.
- AI 추천 실패의 직접 원인은 설정된 `gemini-2.0-flash`가 2026-06-01 종료된 것이다. 공식 대체 모델 `gemini-3.5-flash`로 변경한다.
- AI 추천은 저빈도 HTTP 요청이므로 동기 응답을 유지하되 요청자가 ACTIVE room member인지 DB에서 검증한 후 대화 로그를 조회한다.

## 2026-07-11 Professional Theme And AI Recommend Result

- 테마 변경은 CSS를 중심으로 수행했으며 기존 컴포넌트 계층, 상태, API, WebSocket 흐름은 유지했다.
- 공통 토큰을 `index.css`에 두고 각 화면은 의미 기반 토큰을 참조한다.
- ChatBox 업로드 원형에 박혀 있던 노랑 인라인 값만 CSS 변수로 교체했다.
- AI 추천 모델은 종료된 Gemini 2.0 Flash에서 Gemini 3.5 Flash로 변경했다.
- Gemini 공급자 오류 로그는 model, status, response body를 포함한다.
- 반응형 레이아웃은 구현하지 않고 후속 적용 순서와 검증 해상도만 troubleShooting 문서에 기록했다.
- `npm.cmd run build`는 성공했으며 선행 ESLint warning만 남았다.
- AI MyBatis XML 파싱과 `git diff --check`는 성공했다.
- 로컬 개발 서버가 실행 중이지 않아 브라우저 시각 검증은 수행하지 못했다.
- `plan.md` 원칙에 따라 Gradle과 project refresh는 실행하지 않았다.

## 2026-07-11 Join Route And Draft Group Duplicate Creation Decisions

- `/login`과 `/join`은 비로그인 사용자가 접근해야 하는 공개 경로다. AppShell의 비로그인 redirect는 이 두 경로를 제외한다.
- 그룹 draft 첫 메시지는 방 생성 명령이므로 응답을 받기 전 동일 draft에서 두 번째 START 요청을 허용하지 않는다.
- React state 갱신만으로는 같은 event loop에서 연속 Enter를 막지 못하므로 `useRef`를 동기 잠금으로 사용하고 state는 버튼 UX 표시에 사용한다.
- 일반 방의 SEND_MESSAGE 연속 전송 정책은 이번 문제와 별개이므로 변경하지 않는다.

## 2026-07-11 Join Route And Draft Group Duplicate Creation Result

- RouteBody의 `/join` 등록과 LoginPage의 이동 버튼은 원래 정상이라 변경하지 않았다.
- AppShell 인증 redirect 조건에 공개 경로만 추가했다.
- draft START 요청에만 ref 잠금을 적용해 기존 일반 SEND 동작은 유지했다.
- `npm.cmd run build`와 `git diff --check`는 성공했다.
- Gradle과 project refresh는 실행하지 않았다.

## 2026-07-11 Join Profile And Media Type Decisions

- 이미지 미선택 가입은 파일 전송이 없으므로 JSON 요청을 사용하고 서버가 기본 프로필 경로를 저장한다.
- 실제 이미지가 선택된 경우에만 multipart/form-data를 사용한다.
- `/user/join`은 같은 URL에서 consumes 값으로 JSON과 multipart handler를 구분한다.
- 회원가입 화면에는 파일 선택 버튼 하나만 두고 별도 기본 이미지 복귀 버튼은 제거한다.
- 현재 8080 프로세스는 multipart 요청을 `RequestResponseBodyMethodProcessor`로 처리하려 해, source가 아니라 이전 `@RequestBody` controller가 실행 중임을 확인했다.
- `npm.cmd run build`와 수정 파일 대상 `git diff --check`는 성공했다.
- `plan.md` 원칙에 따라 Gradle 명령은 실행하지 않았다. Java 변경 반영에는 domain-service 컴파일과 재시작이 필요하다.

## 2026-07-11 Notice Width And First Message Decisions

- 활성 공지 bar는 채팅창 좌우 8px만 남기고 가로 폭을 채운다. 접힘 상태에서도 같은 box와 오른쪽 toggle 좌표를 유지한다.
- START 응답은 이미 durable save가 끝난 `firstChatMessage`를 포함하므로 이를 송신자 화면의 초기 메시지 source로 사용한다.
- 새 방의 DB insert는 Kafka worker가 비동기로 처리하므로 START 직후 HTTP 메시지 조회 결과는 source of truth로 화면을 덮어쓰면 안 된다.
- HTTP 로드 메시지와 START/WS 메시지는 messageId로 병합하고 현재 메모리 메시지를 보존한다.
- 기존 REST, WebSocket, Kafka, backend transaction 구조는 변경하지 않는다.

## 2026-07-11 Notice Width And First Message Result

- 공지 bar와 toggle은 펼침/접힘 모두 동일한 좌우 좌표를 사용한다.
- START 응답의 firstChatMessage는 Redux window state를 거쳐 새 ChatBox에 전달된다.
- 초기 HTTP 조회와 WS broadcast는 messageId 병합으로 첫 메시지 유실과 중복을 모두 방지한다.
- 첫 메시지는 Kafka DB insert 완료를 기다리지 않고 durable save가 끝난 START 응답으로 렌더된다.
- `npm.cmd run build`와 `git diff --check`는 성공했다.
- Gradle과 project refresh는 실행하지 않았다.

## 2026-07-11 Compact Layout Decisions

- 이번 작업은 기능 흐름, API, WebSocket, backend 구조를 변경하지 않고 CSS 레이아웃 수치만 조정한다.
- 헤더 높이는 사용자 요청대로 100px에서 85px로 낮추고, 내부 프로필 이미지와 버튼도 같은 밀도에 맞춰 줄인다.
- 친구 목록 화면은 3열 구조를 유지하되 패널과 내부 scroll 영역 높이만 낮춰 브라우저 세로 스크롤을 줄인다.
- 채팅방 목록은 기존 폭의 약 60%인 434px로 줄이고 `margin-left`를 8px만 둬 왼쪽에 가깝게 붙인다.
- 채팅창은 고정 폭을 유지하면서 메시지 영역과 입력 영역 높이를 줄이고 `max-height: calc(100vh - 24px)`로 viewport 침범을 제한한다.
- 프로필 이미지 파일명이 임의 문자열처럼 보이는 이유는 저장 파일명이 충돌 방지를 위해 uuid/hash 기반으로 바뀌기 때문이다. 원본 파일명을 그대로 보여주려면 업로드 시 original file name을 별도 컬럼이나 response 필드로 보존해야 한다.

## 2026-07-11 Compact Layout Result

- `Header.css`, `Friends.css`, `ChatList.css`, `ChatBox.css`만 레이아웃 목적으로 수정했다.
- `ChatList.css`의 `.chatListTitle`은 ChatBox title과 전역 클래스명이 겹치므로 `.ChatListContainer .chatListTitle`로 스코프를 좁혔다.
- 작업기록은 `cdx0711.md`에 남겼다.
- `npm.cmd run build`는 성공했으며 기존 ESLint warning만 남았다.
- `git -c safe.directory=D:/castleDragonProjects/castleChat diff --check`는 성공했으며 CRLF 변환 warning만 출력됐다.

## 2026-07-11 Discord Style UI Conversion Decisions (claude)

디자인 리드 : 채팅 제품 UI의 "보존형 리디자인". 목적은 비전공 인사팀 첫인상 + 웹 관례 준수. 배경은 chatUI.md 참고.
다이얼 : VARIANCE 3(정돈된 2패널) / MOTION 2(hover, active만) / DENSITY 6(목록은 밀도 있게).

1. 레이아웃은 "새로 만들지 않고 도킹"으로 해결한다.
   기존 ChatBox(플로팅 창)는 그대로 두고 isDocked prop 하나로 고정 좌표+드래그만 끈다. 도킹 시 .docked CSS 오버라이드로 메인 패널을 100% 채운다.
   이유 : ChatBox 2318줄에 메시지/첨부/리액션/공지 전 기능이 들어있어 레이아웃 때문에 재작성하는 건 리스크만 크다.
   부수 효과 : 팝아웃 재도입 시 isDocked=false로 그대로 복구된다.
2. chatWindowsSlice는 "활성 방 1개" 정책으로 변경한다.
   openChatWindow에서 push 대신 배열 교체. A 보다가 B를 열면 A의 ChatBox 언마운트(EXIT_ROOM 자동) 후 B 마운트(ENTER_ROOM).
   기존 멀티창에서도 창 닫기가 같은 흐름이라 서버 프로토콜 변화 없음. x/y/zIndex 필드는 팝아웃 복구 대비 유지.
3. 방 목록은 사이드바가 된다. "채팅" 버튼 제거, 행 전체 클릭 입장(디코/슬랙 관례). handleEnterRoom 로직 재사용. 활성 방 하이라이트는 chatWindows read-only 참조.
4. 녹색 테마는 index.css 토큰 교체로 끝낸다. 전 컴포넌트가 var(--castle-*) 참조라 토큰만 바꾸면 전체 리테마.
   teal(#0f766e) -> 저채도 emerald(#047857). nav는 순검정 대신 짙은 녹회색. danger 빨강 unread 뱃지는 의미색이라 유지.
5. 반응형은 범위 제외. Header 자체가 1500px 고정이라 채팅 페이지만 반응형해도 의미 없음. Header 개편과 함께 별도 과제.
6. ChatBox 내부 노란 하드코딩 색(#fee500, #fff8c7 등) 잔존. 이번엔 도킹 레이아웃 검증이 우선이라 후속으로 미룸(한 번에 다 바꾸면 회귀 추적 어려움).

## 2026-07-12 Discord Chat UI Regression Fix Decisions

- 이번 작업은 디스코드형 2패널 구조를 유지하면서 기존 채팅 기능의 회귀만 복구한다.
- 메시지 추가와 리액션 변경을 구분한다. 새 메시지 또는 최초 입장 때만 최하단 이동을 허용하고, 기존 메시지 객체의 리액션 변경은 현재 scrollTop을 보존한다.
- 답장 대상 이동은 messageId 기반 DOM 식별자로 처리하며, 대상이 현재 로드 범위에 없을 때는 잘못된 위치로 이동하지 않는다.
- 상단 bar, 우클릭 메뉴, 공지, 방 메뉴는 viewport가 아니라 채팅방 root를 좌표 기준으로 삼는다.
- unread와 feed 복원은 기존 WebSocket 및 HTTP 계약을 우선 재사용하고, 기능을 새로 추측해 만들지 않는다.
- Gradle과 project refresh는 실행하지 않는다. 프론트 변경은 npm build로 검증한다.

## 2026-07-12 Discord Chat UI Regression Fix Result

- 입력 영역은 첨부 버튼 70px과 전송 버튼 86px을 고정하고 textarea가 남은 폭을 차지한다.
- 상단 bar는 46px 고정이며 도킹 레이아웃의 메시지 증가에 의해 축소되지 않는다.
- 자동 스크롤은 새 메시지와 최초 입장에만 반응하고, 리액션과 읽음 상태 변경은 현재 위치를 유지한다.
- 답장 미리보기는 현재 로드된 원본 메시지로 이동하고 잠시 강조한다.
- 이전 방 EXIT 누락과 route 이탈 viewing registry 잔류를 FE에서 명시적으로 정리했다.
- room feed는 room action 트랜잭션에서 SYSTEM 메시지로 동기 저장한다. Kafka 비동기 메시지 경로는 변경하지 않았다.
- 절대좌표 UI는 도킹 채팅방을 기준으로 복구했고, room menu는 목록 영역을 덮는 drawer로 배치했다.
- 프론트 build, MyBatis XML 파싱, diff check는 성공했다. Gradle과 DB ALTER는 실행하지 않았다.
