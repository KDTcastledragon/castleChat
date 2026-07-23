# Context Notes

## 2026-07-15 gRPC generated source and AI test recovery

- `RoomFeedResponse.target_members` is already correct in `chengine.proto` and both converters expect that field.
- The generated Java class is stale and lacks `getTargetMembersList()` and `Builder.addTargetMembers()`; Gradle Refresh and Project Clean do not regenerate proto sources.
- `AiRecommendServiceTest` had two imports after the closing class brace. This was a Java syntax error, not a missing JUnit or Mockito dependency.
- Per `plan.md` prpr8, Codex did not run Gradle. The user must run the proto generation and compilation command after this source repair.

## 2026-07-15 채팅 UI 및 AI Nginx 경로 수정

- 배포 화면의 origin은 `http://localhost`이며 9200 직접 호출은 cross-origin이므로, React는 `/aiRecommend` 상대 경로를 사용하고 Nginx가 9200으로 프록시해야 한다.
- 기존 `ChatBox` 상태와 공용 확인 모달을 재사용하며 새 UI 라이브러리는 추가하지 않는다.
- 채팅방 메뉴는 630px 이내의 2열 구조로 바꾸고 왼쪽 하단에 방 나가기, 오른쪽 하단에 초대하기를 고정했다.
- 과거 메시지를 보는 동안 새 메시지는 목록에 추가하되 자동 스크롤하지 않고, 하단 미리보기와 이동 버튼만 표시한다.
- CORS 로그는 소스가 아니라 Nginx가 서빙하던 이전 빌드 번들의 `http://localhost:9200` 호출이 원인이었다. 새 빌드는 상대 경로만 포함한다.
