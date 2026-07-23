# Checklist

## 2026-07-15 gRPC generated source and AI test recovery

- [x] Confirm `target_members` exists in `chengine.proto`.
- [x] Confirm generated `RoomFeedResponse.java` lacks the new accessors.
- [x] Remove imports appended after `AiRecommendServiceTest` class.
- [ ] Regenerate common-contract proto and compile affected modules.

## 2026-07-15 채팅 UI 및 AI Nginx 경로 수정

- [x] 확인 모달 버튼 순서를 확인 왼쪽, 취소 오른쪽으로 통일한다.
- [x] 메시지 리액션 버튼과 채팅방 메뉴 레이아웃을 수정한다.
- [x] 과거 스크롤 중 하단 이동과 새 메시지 표시를 추가한다.
- [x] 친구 정보 영역 우클릭 메뉴로 변경한다.
- [x] AI 호출을 Nginx 상대 경로로 통일하고 최소 검증한다.
