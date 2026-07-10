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
