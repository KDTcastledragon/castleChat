## =========================< [ CLAUDE ] 26.07.15 / 04:27:25 >=============================================
1. 권한 변경 성공시에, "~님의 권한을 ~로 변경하였습니다" 멘트 모달 하나 띄워줘. esc로 닫히게 하고.

2. 채팅 페이지 -> 채팅방 목록에서 120개쌓인 채팅방1 접속 -> 읽음 -> exit room -> F5 새로고침 -> 채팅방목록의 채팅방1에 메시지가 여전히 120개쌓인 것으로 보임. 이후 다시 새로고침 -> 여전히 120개 쌓인것으로 보임. (방금 읽었음에도.) 버그 해결하셈.

3. [ROOM_FEED]를 앞에 붙이고 메세지를 "전송"하면(sendMsg), room feed메시지로 보이지 않고(invite,kick등의 주요 feed noti), user의 sendMsg처럼 보임. 공지사항에도 올라가지 않음. 그런데, 채팅방 메뉴의 공지사항메뉴를 클릭할 시, 공지사항 목록에서, 채팅방공지로 설정하지 않았음에도 공지사항에 올라가있음. 수정/재공지/삭제 버튼 아예 존재하지않음. 

예시) "[ROOM_FEED] 테스트투투투" 를 입력창에 그대로 치고 sendMsg -> 일반 Msg처럼 그대로 뜸. -> 공지사항 메뉴 확인 -> 공지사항 목록에 senderNickName과 함께 "[ROOM_FEED] 테스트투투투"라고 그대로 뜸. 다른 공지사항과 다르게 수정/재공지/삭제 메뉴 자체가 존재하지 않음.

"[ROOM_FEED]내가곧신이다!!![ROOM_FEED]내가곧신이다!!![ROOM_FEED]내가곧신이다!!!" << 이런식으로 메세지를 보내고 exit room -> enter room 하면 >> "내가곧신이다!!![ROOM_FEED]내가곧신이다!!![ROOM_FEED]내가곧신이다!!!" 이렇게 피드가 올라와있음....어이가 없네......

[ROOM_FEED]와 같은 MSG_TEXT의 STRING이 아니라, 아예 다른 TYPE으로 구분해야할텐데..왜 이따구로 STRING기준 분류를 하는건지???? 이번꺼는 원인 `확실히` 파악하고 버그 해결하셈.

4. 채팅방 메뉴 가로크기를 120px만 더 크게하자.
채팅방 메뉴의 채팅방 멤버 리스트에서, 닉네임 <-> 권한 <-> 추방버튼 <-> 더보기버튼 간격 좀 일정하게 맞추자...현재는 너무 뒤죽박죽이다.

5. 이미 리액션이 생성된 메시지에는 이모티콘추가 버튼이 생겼으므로, hover해도 이모티콘 추가 버튼 안 뜨게 하자.

6. urc와 sendMsg 시각은 무조건 msg 바로 옆에 붙어잇게 하자. margin은 3px 주자.
리액션이 추가되도 리액션과 같이 밑으로 밀리지 않게 해. 위치는 항상 msg 바로옆 고정. 

7. 6번의 영향과 관계없이, msg에 hover 시 이모티콘 추가 버튼이 뜨는건 현재 위치 그대로 고정시키자.
hover할때마다 urc가 밀리거나 반대로 이모티콘추가버튼이 엉뚱한데로 밀리는 경우가 없게 하라는 얘기야.

8. 상대 프로필 눌렀을 시 뜨는 모달창 크기 510*225 정도로 키우자. 이 크기에 맞춰서 profile Img 랑 버튼 크기도 키우고. 닫기 버튼은 x로 보기좋게 변경.

9. 친구목록에서, 상대 클릭시 (채팅버튼/체크박스 제외) 친구삭제/친구차단 메뉴 띄워줘. (msg 메뉴처럼)
그리고, 체크박스 크기 좀 더 키워줘.

10. 내정보 전체 박스 크기 꽉차게 키우자. 하나의 페이지처럼 보이게 해야돼. 스크롤바 당연히 생기면 안되고.

11. 닉네임 변경 << 코멘트와 현재 버튼 지워. 그리고, 닉네임 변경하기 버튼을 눌러서 변경 활성화/비활성화 toggle 기능으로 하자. 프로필 이미지변경 버튼도 지우고, 기본이미지 옆에 원래대로 버튼(이전 이미지. ctrl+z같은 느낌..뭔지 알지? 아주 간단히 구현해.)도 넣자.
 
비밀번호 변경 자리에, 프로필 저장 버튼을 넣고(닉네임 프로필 통합 저장. 둘 중 하나라도 변경되야 활성화됨.), 비밀번호 변경하기 버튼은...너가 적절한 곳에 잘 배치해봐.


12. 설정 메뉴에...뭘 넣어야 되지? 카톡도 아니고 ..그냥 토이프로젝트라서 뭐 넣을게 없네? demo식으로 간단하게 구색만 갖춰봐. (여기에는 토큰 많이쓰지마. 최대한 간단하게 아껴써.)


## =========================< [ CLAUDE ] 26.07.15 / 04:27:25 >=============================================

# 2026-07-15 채팅웹 버그 5건 + UI 개선 7건 (지시사항 1~12)

## 1. 권한 변경 성공 모달

- **원인(요구)** : 권한 변경 성공 시 아무 피드백이 없어서 성공 여부를 멤버 리스트의 role 표기 변화로만 추측해야 했다.
- **해결방법** : `ChatBox.jsx`의 WS 핸들러에서 `ROOM_MEMBER_ROLE_CHANGED` 수신 시, `feed.requesterPublicId === myPublicId`일 때만 `roleChangeResultModal` state를 세팅해 "{닉네임}님의 권한을 {ROLE}로 변경하였습니다." 모달을 띄운다. ESC(기존 handleEscKeyDown 체인의 최우선 순위로 추가) / 오버레이 클릭 / 확인 버튼으로 닫힌다. 방 이동 시 state 리셋에도 추가.
- **의도** : emitWsChangeMemberRole은 fire-and-forget(emitWs)이라 요청 시점이 아닌 **서버 브로드캐스트 수신 시점**을 성공 판정 기준으로 삼았다. 실제로 DB update가 완료된 뒤에만 feed가 오므로 거짓 성공 표시가 없다.
- **tradeoff** : 요청자가 아닌 다른 멤버에게는 모달을 띄우지 않고 기존 feed 메시지로만 알린다(모두에게 모달을 띄우면 방해). 실패 시에는 기존 alert 흐름 유지.

## 2. 읽은 방인데 F5 후 unread 120 부활 (원인 3중첩 — 확정 진단)

- **원인** :
  1. **`channel-engine/application.properties`의 `chat.read-position.flush-delay-ms=500000`** — dirty read-position을 DB로 내리는 `ReadPositionFlushWorker`가 **8.3분에 1회**만 실행되고 있었다. 읽음 위치는 Redis(`chat:room:{roomId}:read-position`)에만 갱신되고 dirty 큐(`chat:dirty:read-position`)에 마킹만 된 상태로 대기하는데, 8.3분 안에 PC를 끄면(=Redis 컨테이너 종료) 읽음 위치와 dirty 큐가 **함께 소실**된다. 이후 재부팅하면 warmUp이 DB의 옛 LRM을 Redis로 복원 → 채팅방 목록 SQL이 옛 LRM 기준으로 unread를 세서 120이 부활.
  2. 07-14에 넣었던 domain-service의 Redis LRM 보정(`RoomQueryService`)이 **STS 재빌드가 안 되어 미적용 상태**였다. (bin/main 클래스 컴파일 시각 18:16 < 소스 수정 시각 21:47. 외부 편집이라 Eclipse workspace refresh가 안 됨.)
  3. 진단 근거(라이브 확인) : DB `room_members`에서 user 5의 room 4 LRM이 110개 이전 메시지로 정체 / Redis 값도 DB와 완전 동일(=재시작 후 warmUp 복원본) / Redis 컨테이너 StartedAt = 07-15 02:13(재부팅 시각) / dirty 큐에 수동 항목을 넣고 8초 대기해도 소비 안 됨(500초 주기 확인).
- **해결방법** : `chat.read-position.flush-delay-ms=500000 → 5000`(5초). 유실 창을 8.3분 → 5초로 축소. properties에 재발 방지 주석 명시.
- **의도** : prpr 4(readMessage는 비동기 batch flush) 구조 자체는 유지한다. write-behind는 원래 "짧은 유실 창"을 전제로 한 설계인데 주기 설정값이 그 전제를 깨고 있었던 것이므로, 구조 변경 없이 설정만 원복하는 것이 최소·정답 수정이다.
- **tradeoff** : write-behind인 이상 마지막 5초의 읽음 위치는 Redis 재시작 시 여전히 유실될 수 있다(unread 몇 개가 잠깐 다시 보이는 수준). 완전 무손실을 원하면 Redis AOF 활성화 또는 동기 DB 반영이 필요하지만, prpr 4(1:n 동기 CRUD 금지) 위배 + 토이 규모에서 불필요하다고 판단.
- **주의** : domain-service·channel-engine 모두 **STS에서 Refresh(F5) 후 재기동해야 반영**된다. MyBatis XML/properties는 기동 시 1회만 읽는다.

## 3. [ROOM_FEED] string 분류 폐기 → message_type ENUM 'SYSTEM' 분리 (근본 수정)

- **원인** : room feed(초대/강퇴/권한변경/공지 알림)가 `chat_messages`에 `message_type='TEXT'` + `message_text='[ROOM_FEED]...'` **문자열 prefix로 저장**되고, 조회 시 `LIKE '[ROOM_FEED]%'`로 분류되고 있었다. 그 결과:
  - 유저가 입력창에 "[ROOM_FEED]xxx"를 직접 쳐서 보내면 → 저장은 일반 TEXT인데, 재입장 시 조회 쿼리/FE 정규화가 prefix를 보고 feed로 오분류. "[ROOM_FEED]A[ROOM_FEED]B" 같은 메시지는 첫 prefix만 잘려 "A[ROOM_FEED]B"로 표시.
  - 공지 목록에 떠 있던 "[ROOM_FEED]호롤..." 항목은 DB `chat_room_notices`에 실존하는 행(room_notice_id=14, requester=user 2). 유저가 친 fake 메시지를 계정 2가 컨텍스트 메뉴 "공지"로 등록한 것. 조회 계정이 작성자가 아니라 수정/재공지/삭제 버튼이 안 뜬 것으로 **이건 정상 동작**. prefix 문구가 feed처럼 보여서 버그로 오인된 케이스.
- **해결방법** (type 기반으로 전면 교체) :
  1. DDL : `ALTER TABLE chat_messages MODIFY message_type ENUM('TEXT','IMAGE','FILE','VIDEO','AUDIO','SYSTEM') NOT NULL DEFAULT 'TEXT'` — **실행 완료**.
  2. 마이그레이션 : prefix 행 18개를 `message_type='SYSTEM'` + prefix 제거로 UPDATE — **실행 완료**.
  3. channel-engine `RoomMapperXml.insertRoomFeedMessage` : `'TEXT'` + `CONCAT('[ROOM_FEED]',...)` → `'SYSTEM'` + 순수 feedText.
  4. domain-service `ChatMapperXml.loadMessagesInRoom` : CASE WHEN LIKE 분류 제거, 컬럼 그대로 select.
  5. domain-service `RoomMapperXml.getMyAllChatRooms` : lastMessage 서브쿼리의 prefix 제거 CASE 삭제, unread LEFT JOIN 및 `countUnreadMessages`의 `NOT LIKE '[ROOM_FEED]%'` → `message_type != 'SYSTEM'`.
  6. websocket-gateway `WsGateChatHandler.defaultMessageType` : 클라이언트 전송 messageType을 화이트리스트(TEXT/IMAGE/FILE/VIDEO/AUDIO)로 제한. **클라이언트가 'SYSTEM'을 위조 전송해 가짜 feed를 만드는 것 차단**(신뢰 경계 검증).
  7. FE : `chatApi.js`의 normalizeRoomFeedMessage 삭제, `ChatList.jsx`의 isRoomFeedPreview 분기 삭제. (feed는 MSG_CREATED/CHAT_ROOM_UPDATED로 오지 않으므로 FE에서 prefix를 볼 일 자체가 없어짐.)
- **의도** : "데이터 분류는 문자열이 아니라 타입 컬럼으로"가 원칙. 유저 입력 텍스트는 어떤 내용이든 절대 시스템 메시지로 오인되지 않아야 한다. feed를 chat_messages에 같이 두는 것 자체는 유지 — 타임라인 정렬/페이징 커서(message_id)와 읽음 위치(LRM) 커서를 단일화하기 위한 표준 설계(Discord/Slack 동일 패턴).
- **tradeoff** :
  - 기존 fake 테스트 메시지("[ROOM_FEED]내가곧신이다..." 등)는 실제 feed와 구분이 불가능해서 마이그레이션 때 일괄 SYSTEM으로 변환됨. 과거 fake 행들은 feed처럼 보이게 됨(테스트 쓰레기 데이터라 허용).
  - 공지 14번의 "[ROOM_FEED]호롤..." 문구는 공지 테이블 데이터라 그대로 둠(삭제는 파괴적 조작이라 미실행). 거슬리면 작성 계정(공성전차)으로 삭제하면 됨.
  - feedText가 완성된 한국어 문장으로 저장되는 구조(닉변 시 과거 feed에 옛 닉 잔존, 다국어 불가)는 남은 설계 부채지만 현 규모에선 수정 불필요로 판단.
- **근데,,FeedMsg를 chatMsg와 동일 취급하는것이 맞는가?** :
  - **결론 : 맞다. 같은 테이블 + 타입 컬럼 구분이 채팅 도메인의 표준 설계다.** 실제로 Discord는 멤버 입장/고정 알림 등을 messages 테이블에 `message.type`(GUILD_MEMBER_JOIN, CHANNEL_PINNED_MESSAGE 등)으로, Slack은 `subtype`(channel_join, channel_leave 등)으로 넣는다. "데이터의 출처(유저 vs 시스템)"는 다르지만 **소비되는 위치(채팅 타임라인)**가 같기 때문이다.
  - **근거 1 — 정렬/페이징 커서 단일화** : feed는 일반 메시지 사이에 시간순으로 끼어서 렌더돼야 한다. 테이블을 `room_feeds`로 분리하면 방 입장/무한스크롤 때마다 `chat_messages UNION room_feeds` 후 재정렬이 필요하고, 현재의 `message_id < ? ORDER BY message_id DESC LIMIT 50` 커서 페이징이 두 테이블에 걸쳐 깨진다(각 테이블에서 50개씩 뽑아 병합 후 50개 자르고 다음 커서를 테이블별로 따로 관리해야 함). 같은 테이블이면 Snowflake message_id 하나가 정렬키+페이징 커서를 겸한다.
  - **근거 2 — 읽음 위치(LRM) 커서 단일화** : `last_read_message_id`는 message_id 축 위의 단일 커서다. feed가 별도 테이블이면 "초대 알림까지 읽었는지"를 추적할 두 번째 커서가 필요하거나, feed가 읽음 개념에서 아예 빠진다. 같은 테이블이면 feed도 자연스럽게 커서 아래/위로 구분되고, unread 계산에서만 `message_type != 'SYSTEM'`으로 제외하면 끝난다.
  - **근거 3 — 파이프라인 공유** : 저장 위치만 다를 뿐 broadcast, 방 입장 시 최근 50개 로드, lastMessage 미리보기까지 전부 기존 메시지 경로를 재사용한다. 분리하면 이 각각에 feed 전용 경로가 하나씩 더 생긴다.
  - **어제까지의 진짜 문제는 "같은 테이블"이 아니라 "같은 타입(TEXT) + 문자열 prefix 구분"이었다.** 유저 입력값과 시스템 데이터가 같은 값 공간(message_text)에서 구분 불가능해진 것이 버그의 원인이고, 이건 SYSTEM 타입 분리로 해소됐다. 타입으로 구분되는 지금은 언제든 `WHERE message_type != 'SYSTEM'` 한 줄로 걸러낼 수 있으므로 "동일 취급"이 아니라 "같은 저장소, 다른 타입"이다.
  - **분리가 이기는 조건 (해당되면 그때 분리)** : ① feed 보존 기간을 메시지와 다르게 가져갈 때(메시지 영구 + feed 30일 등), ② feed 볼륨이 메시지를 압도해 타임라인 인덱스를 오염시킬 때, ③ feed를 구조화 데이터(feed_type + target_user_ids)로 저장해서 렌더 시점에 문구를 조립(닉변 반영/다국어)해야 할 때. 현재 규모(feed 18행)에선 셋 다 해당 없음.
  - **면접 대응** : "왜 시스템 알림을 메시지 테이블에 넣었나?" → "타임라인에 시간순으로 끼어 보여야 하는 데이터라서 정렬·페이징 커서(Snowflake message_id)와 읽음 위치 커서(last_read_message_id)를 메시지와 공유하는 게 필수였다. 테이블을 나누면 UNION 페이징과 이중 읽음 커서가 생긴다. 대신 타입 컬럼으로 구분해 unread 집계에서 제외했고, Discord/Slack도 같은 구조다."


## 4. 채팅방 메뉴 +120px & 멤버 리스트 정렬

- **원인(요구)** : 메뉴 폭이 좁고, 멤버 행의 닉네임/권한/추방/더보기 간격이 행마다 제각각.
- **해결방법** : docked `.roomSidePanel` 폭 `min(380px,76%) → min(500px,76%)`(+120). 비도킹 min-width 330→450. 친구 초대 패널 left도 `calc(min(500px,76%) + 10px)`로 동기화. `.roomMemberItem` grid를 `30px | minmax(0,1fr) | 64px | 76px | 92px`, gap 10px 고정폭 열로 통일. `.memberRoleChangeDetails` width 100%(열에 맞춤), `.memberDangerActions` 중앙 정렬.
- **의도** : flex가 아닌 고정폭 grid 열이라 권한/버튼 유무와 무관하게 모든 행의 열 위치가 동일해진다(placeholder div가 빈 열을 채움).

## 5~7. 메시지 라인 재구성 (hover 이모티콘 버튼 / urc·시각 위치 고정)

- **원인** : 기존 DOM이 `chatRow > [messageInfo][hoverBtn][messageContent(말풍선+리액션바)]` 구조 + `align-items:flex-end`라서, 리액션 바가 생기면 messageContent 전체 높이가 늘어나 urc/시각이 리액션 높이만큼 아래로 밀렸다. hover 버튼도 말풍선과 urc 사이에 끼어 있어 urc가 말풍선에 바로 붙지 못했다.
- **해결방법** : `messageContent` 내부에 `.messageBubbleLine`(flex, align-items:flex-end, **gap 3px**) 라인을 신설하고 [hover버튼][messageInfo][말풍선](mine 기준, other는 대칭)을 이 라인에 배치. 리액션 바는 라인 **아래** 형제로 분리. hover 버튼은 기존처럼 opacity 0→1 (공간은 항상 점유)이라 hover 시 아무것도 밀리지 않고, **리액션이 이미 있는 메시지는 hover 버튼을 아예 렌더하지 않음**(리액션 바에 추가 버튼이 이미 있으므로 중복 제거 — 지시 5). 버튼이 말풍선 반대편 끝에 있어서 렌더 여부가 urc↔말풍선 간격에 영향 없음.
- **의도** : urc/시각은 "말풍선의 속성"이므로 말풍선과 같은 라인에 묶는 게 구조적으로 맞다. margin 3px는 flex gap으로 구현(요소별 margin보다 단순).
- **tradeoff** : messageContent max-width 250px 제한을 100%로 풀고 말풍선(messageWrap)의 250px 제한만 유지. `.mine .messageContent { align-items:flex-end }` 추가로 내 메시지의 리액션 바가 오른쪽 정렬됨(기존은 왼쪽에 붙었음 — 부수 개선).

## 8. 프로필 모달 510×225 확대

- **해결방법** : `.profilePopup`을 510×225 가로형 flex로 변경 — 좌측 프로필 이미지 140px 원형, 우측 `profilePopupMainInfo`(닉네임 22px + 버튼 라인). 액션 버튼 120×42/글자 14px로 확대. 닫기 버튼은 "닫기" 텍스트 → `×`(22px, 투명 배경, hover 진하게, aria-label 유지).

## 9. 친구목록 컨텍스트 메뉴 + 체크박스 확대

- **해결방법** : `Friends.jsx`에 friendContextMenu state 추가. friendItem 클릭 시(단, `e.target.closest('button')`/`input`이면 무시 → 채팅 버튼·체크박스 클릭과 충돌 없음) 클릭 좌표 기준으로 msg 메뉴와 같은 패턴의 메뉴(친구삭제/친구차단)를 FriendListContainer 기준 absolute로 표시. 바깥 클릭/ESC로 닫힘. 체크박스 20×20 + accent-color 테마색.
- **의도/한계** : **친구삭제/차단 BE API가 아직 없음**(FriendController에 add/respond/목록 조회만 존재). 따라서 메뉴 선택 시 confirm 후 "아직 준비 중인 기능입니다" 안내까지만 구현. BLOCK_FRIEND(plan.md B-1의 3번)가 구현되면 handleFriendMenuAction의 alert 자리에 API 호출만 끼우면 된다.
- **tradeoff** : BE API 없이 메뉴 UI를 먼저 붙였다. (얻은 것) 지시받은 진입점 UX가 즉시 완성되고, BLOCK_FRIEND 구현 시 alert 자리에 API 호출만 끼우면 되는 구조. (잃은 것) 그때까지는 눌러도 실제로 삭제/차단되지 않는 메뉴가 유저에게 노출됨 — "완성본 기준" 관점에선 미완성 기능의 노출이다. 대안 두 가지(① BE API까지 지금 신설 — prpr 9상 지시 범위 밖 + BLOCK은 PENDING 자동거절/수신차단 등 부수효과 설계가 필요해서 즉흥 구현 부적절, ② 메뉴 자체를 API 완성 뒤로 미룸 — 이번 지시 불이행) 중 ①②를 포기하고 UI 선행을 택했다.

## 10. 내정보 풀사이즈

- **해결방법** : `.myPageContainer` 1100px 고정 → 100%. `.myPageCard` 720px → 100%×100%, `display:flex; flex-direction:column`. 저장 버튼 섹션(`.mySaveSection`)을 `margin-top:auto`로 하단 고정 → 한 페이지처럼 꽉 참. container `overflow:hidden` 유지로 스크롤바 없음.
- **tradeoff** : 내용이 세로로 희소해질 수 있으나 "하나의 페이지처럼"이 요구사항이므로 수용.

## 11. 닉네임/프로필 저장 UI 재구성

- **해결방법** :
  - "닉네임 변경" label 코멘트/기존 변경 버튼 제거 → label "닉네임" + input은 평소 disabled로 현재 닉네임 표시. "닉네임 변경하기" 버튼이 편집 활성/비활성 **토글**(활성화 시 현재 닉네임을 초안으로 로드, 비활성화 시 초안 폐기 = 변경 취소).
  - "프로필 이미지 변경" 저장 버튼 제거. "기본 이미지" 옆에 "**원래대로**" 버튼 추가 — 선택한 초안(profileImg draft)을 버리고 기존 프로필로 복귀(ctrl+z 컨셉, state 초기화 한 줄로 구현).
  - 비밀번호 변경 자리에 "**프로필 저장**" 버튼 — `isNicknameChanged || isProfileImgChanged`일 때만 활성. 닉네임/이미지를 mutateAsync로 순차 통합 저장 후 단일 alert. 개별 mutation의 성공/실패 alert는 제거(중복 알림 방지), 에러는 저장 함수의 catch에서 일괄 처리.
  - 비밀번호 변경하기 버튼은 헤더 우측(`margin-left:auto`)으로 이동. 모달 동작은 기존 유지.
- **의도** : "변경됨" 판정을 실제 값 비교(`nickname.trim() !== me.nickname`, `profileImg !== me.profileImg`)로 해서 무의미한 저장 요청을 버튼 단계에서 차단.
- **tradeoff** : 닉네임과 이미지 API가 분리돼 있어 통합 저장은 순차 2회 호출이다. 하나 성공 후 하나 실패하면 부분 반영될 수 있으나(성공분은 캐시 반영됨), 통합 API 신설은 BE 변경이라 범위 밖으로 판단.

## 12. 설정 메뉴 demo

- **해결방법** : 알림(메시지 알림/알림음), 채팅(Enter 전송), 일반(언어 select) 3섹션. localStorage(`castlechat:settings`)에만 저장하는 demo이며 하단에 "데모 설정, 실제 동작 미연결" 문구 명시.
- **tradeoff** : 실동작 연결 없음(지시가 "구색 + 토큰 절약"). 나중에 방별 알림 설정(BE에 이미 존재)과 연결 가능.

---

## 검증

- FE : 수정한 6개 파일(@babel/parser JSX 파싱) 전부 통과. npm start hot reload로 즉시 반영됨.
- DB : ALTER + 마이그레이션 실행 완료, `message_type` 분포 확인(TEXT 388 / IMAGE 3 / SYSTEM 18).
- BE : 코드/설정 수정만 완료. **빌드·재기동은 prpr 8에 따라 사용자가 직접 수행** — channel-engine(flush 주기+feed insert), domain-service(쿼리+LRM 보정), websocket-gateway(타입 화이트리스트) 3개 Refresh 후 재기동 필요. event-persist-worker 변경 없음.

## =========================< [ CODEX ] 26.07.15 / 08:07:49 >==============================================

## 1. 채팅방 메시지 검색

- 원인. 채팅방 내부에서 이미 로드한 메시지를 탐색할 UI와 이동 기준이 없었다.
- 해결. 상단 검색 버튼을 추가하고 클릭 시 공지 위에 검색바를 표시했다. 현재 로드된 활성 메시지를 대소문자 구분 없이 검색하며 위·아래 버튼 또는 Enter와 Shift+Enter로 결과를 이동한다. ESC는 검색바만 먼저 닫는다.
- 의도. 검색 한 번마다 DB를 조회하지 않고 이미 로드한 메시지를 사용해 대용량 트래픽 원칙을 지킨다.
- tradeoff. 아직 로드하지 않은 과거 메시지는 검색되지 않는다. 서버 전체 검색은 별도 페이징 API가 필요할 때 추가한다.

## 2. AI 추천 목록 닫기

- 원인. 추천 결과를 선택하지 않으면 팝업을 닫을 명시적 수단이 없었다.
- 해결. 추천 목록 우측 상단에 닫기 버튼을 추가하고 추천 배열만 비운다.
- 의도. API 재호출이나 입력 메시지 변경 없이 표시 상태만 정리한다.

## 3. AI 추천 설명 말풍선

- 해결. 추천 버튼 hover와 키보드 focus 시 150×100 말풍선을 버튼 위 50px 지점에 표시했다. 꼬리는 버튼 중앙에 정렬했다.
- 내용. 현재 방 메시지 기반 추천, Gemini 3.5 및 503 가능성, 약 12~20초 소요 안내를 포함했다.
- tradeoff. 모바일에는 hover가 없으므로 현재 데스크톱 웹 기준이다.

## 4. 추방과 영구강퇴 UI

- 원인. 추방과 영구강퇴가 비슷한 위치와 강조도로 노출되어 오조작 위험이 있었다.
- 해결. 추방은 경고색의 일반 버튼으로 정리하고 영구강퇴는 점 세 개 상세 메뉴 안에 숨겼다. 영구강퇴는 위험 경고 모달을 거쳐야 실행된다.
- 의도. 자주 쓰는 추방과 되돌리기 어려운 영구강퇴의 조작 단계를 분리한다.

## 5. 초대 대상자의 채팅 목록 실시간 반영

- 원인. 기존 `ROOM_MEMBER_INVITED`는 현재 방 세션에만 broadcast되어 아직 방에 입장하지 않은 초대 대상자는 이벤트를 받을 수 없었다.
- 해결. Gateway 세션 Registry에 `publicId -> WebSocketSession` 인덱스를 추가했다. 초대 성공 후 응답의 `targetPublicIds` 각각에 `ROOM_INVITED`를 직접 push하고, 대상 FE는 `myAllRooms` 쿼리를 즉시 invalidate한다.
- 의도. proto와 channel-engine 응답을 늘리지 않고 기존 `RoomFeedResponseDTO`를 재사용한다. 대상 조회는 O(1)이다.
- tradeoff. 현재 프로젝트 정책처럼 사용자당 활성 WS 한 개를 기준으로 한다. 향후 다중 기기 동시 접속 정책이면 publicId 값도 세션 Set으로 바꿔야 한다.

## 6. 브라우저 confirm 모달 전환

- 원인. 브라우저 기본 confirm은 테마와 동떨어지고 위치와 표현을 제어할 수 없었다.
- 해결. ChatBox의 권한 변경, 추방, 영구강퇴, 메시지 삭제, 공지 액션, 이미지 다운로드 확인을 하나의 공통 모달로 통합했다. 친구 메뉴에 남아 있던 confirm도 동일한 테마 모달로 교체했다.
- 의도. 새 라이브러리 없이 기존 React state만 사용한다.

## 7. 친구 행 강조

- 해결. 친구 행 hover와 클릭 focus에 테마색 border와 배경을 적용했다. 체크 선택은 `checked`, 행 focus는 `focused`로 분리했다. 친구 행 밖을 누르면 focus만 해제된다.
- 의도. 단톡 선택 상태와 단순 탐색 상태가 섞이지 않게 한다.

## 8. 친구 체크박스 확대

- 해결. 체크박스를 20×20에서 22×22로 확대했다.
- tradeoff. 목록 밀도를 거의 유지하면서 클릭 영역만 소폭 키웠다.

## 9. 내정보 기본 정보 폭 축소

- 해결. 전체 MyPage 박스는 유지하고 기본 정보 grid만 부모 폭의 50%를 사용하도록 변경했다.
- 의도. 짧은 닉네임과 친구코드가 화면 전체 폭을 차지하지 않게 한다.

## 10. 리액션 UI 확대와 30개 선택지

- 해결. 선택창을 360px, 6열 grid로 확대하고 목록창을 390px로 키웠다. 기존 코드를 유지하면서 총 30개의 emoji와 영문 reaction code를 추가했다.
- 의도. emoji가 박스 밖으로 튀어나오는 문제를 막고 DB의 기존 문자열 code 계약을 유지한다.
- tradeoff. 선택지가 많아져 세로 스크롤이 생길 수 있으므로 선택창 높이는 250px로 제한했다.

## 11. 나간 사용자의 프로필 표시

- 원인. 현재 활성 멤버 Map에서 제거된 발신자는 프로필 클릭 대상 객체도 사라졌다.
- 해결. 메시지에 저장된 `senderPublicId`, `senderNickname`, `senderProfileImg`로 fallback 프로필 객체를 만들고 팝업에 전달한다.
- 의도. 방을 나간 것과 회원 탈퇴를 구분한다. 과거 메시지가 가진 발신자 snapshot을 그대로 사용하므로 추가 DB 조회가 없다.

## 12. 메시지 알림 UX

- 해결. 검은 알림을 밝은 CastleChat 테마로 바꾸고 방 이미지, 방 이름, 메시지, 알림 끄기, 닫기 버튼을 배치했다. 알림 본문 클릭 시 해당 방에 입장한다. 최대 4개만 유지하고 각 알림은 4초 뒤 제거한다.
- 알림 끄기. 기존 방 설정 REST를 재사용하며 현재 캐시의 방 이름·썸네일·배경을 함께 보내고 `messageNotificationEnabled=false`로 저장한다.
- 방 이름 조회. `myAllRooms` 캐시를 우선 사용하고 캐시가 없을 때만 한 번 조회해 채운다.
- 서버 검증. 알림 대상 SQL이 이미 `message_notification_enabled = TRUE` 조건을 사용하므로 알림이 꺼진 방은 애초에 push 대상에서 제외된다.
- tradeoff. 방 목록 캐시가 전혀 없는 최초 알림 한 번에는 방 이름 확보용 REST 조회가 추가된다.

## 13. AI 추천 화자 관점 오류

- 원인. `recommendMessages`는 `requesterUserId`를 알고 있고 각 `RecentMessageDTO`에도 `senderId`가 있지만, 현재 `buildRecommendPrompt`는 닉네임과 메시지만 출력한다. Gemini는 누가 추천 요청자인지 알 수 없어 임의 화자의 관점으로 답한다.
- 공동 설계 방향. `buildRecommendPrompt(requesterUserId, recentMessages)`로 요청자 ID를 전달하고 대화 표기를 `요청자(나)`와 `상대 닉네임`으로 구분한다. 출력 규칙에는 반드시 요청자가 지금 보낼 1인칭 발화이며 요청자에게 하는 말은 금지한다고 명시하는 방식이 가장 작고 확실하다.
- 현재 처리. 사용자 요청대로 `AiRecommendService.buildRecommendPrompt`와 그 return 프롬프트는 수정하지 않았다.

## 14. 추가 AI 기능 가이드

- 1순위. 읽지 않은 대화 요약. 긴 방의 미확인 구간만 요약하면 효용이 크고 호출 빈도를 사용자가 통제할 수 있다.
- 2순위. 전송 전 말투 다듬기. 작성한 문장을 정중함, 간결함, 친근함 세 가지로 변환하면 입력 데이터가 짧아 비용이 낮다.
- 3순위. 일정과 할 일 추출. 최근 대화에서 날짜, 약속, 담당자를 구조화해 보여주되 자동 등록은 사용자 확인 뒤에 한다.
- 후순위. 의미 기반 채팅 검색. embedding 저장소가 추가로 필요하므로 포트폴리오 MVP에서는 과하다.
- 제외. 상시 성향 분석과 모든 메시지 자동 분석은 비용, 개인정보, 응답 지연 때문에 현재 무료 API 정책에 맞지 않는다.

## 검증

- 백엔드 소스 흐름. 초대 성공 응답의 `targetPublicIds`가 Gateway publicId 세션 인덱스를 거쳐 `ROOM_INVITED`로 전달되고 FE 전역 handler가 `myAllRooms`를 invalidate하는 경로를 확인했다.
- 알림 설정 흐름. channel-engine 알림 대상 쿼리의 `message_notification_enabled = TRUE` 조건을 확인했다.
- 프론트 테스트. `npm.cmd test -- --watchAll=false --passWithNoTests`를 실행했으나 로컬 `node_modules`에 `react-router-dom`이 없어 테스트 파일 로딩 단계에서 실패했다. 이번 변경의 컴파일 결과를 의미하는 실패는 아니다.
- JSX 문법. Babel parser로 `AppShell.jsx`, `ChatBox.jsx`, `Friends.jsx`, `MyPage.jsx`를 검사했고 네 파일 모두 통과했다.
- 미실행. prpr 8에 따라 Gradle Refresh, Project Clean, `gradlew`는 실행하지 않았다.
