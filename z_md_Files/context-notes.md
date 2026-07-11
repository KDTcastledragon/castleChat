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
