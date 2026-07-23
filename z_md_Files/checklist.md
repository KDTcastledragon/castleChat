# Current Task Checklist

## 2026-07-15 Chat UX And Invite Realtime Follow-up

- [x] 채팅방 로드 메시지 검색과 ESC 닫기
- [x] AI 추천 목록 닫기와 설명 말풍선
- [x] 추방/영구강퇴 UI와 공통 확인 모달
- [x] 초대 대상자의 채팅 목록 실시간 갱신
- [x] 친구 행 선택 강조와 체크박스 확대
- [x] 내정보 기본 정보 영역 폭 축소
- [x] 리액션 선택지 30개와 패널 확대
- [x] 나간 유저 메시지 프로필 열기
- [x] 테마형 메시지 알림, 방 이동, 알림 끄기, 4초/최대 4개
- [x] AI 추천 요청자 관점 고정 프롬프트 반영
- [x] ctxt.md 작업 기록
- [x] 백엔드 이벤트 전달 소스 검증
- [ ] 프론트 테스트 재실행. 로컬 node_modules의 react-router-dom 누락 해결 필요

- [x] 공지 조회 계약과 20개 단위 이력 조회 구현
- [x] 공지 CREATE/INACTIVATE/DELETE 예외 원인 수정
- [x] 알림 설정과 채팅 목록 실시간 갱신 경로 분리
- [x] 상단 고정 공지와 숨김 토글 구현
- [x] 공지 이력 패널과 작성자 전용 액션 구현
- [x] 1:1 채팅 목록 썸네일 가이드 작성만 수행
- [x] 내 리액션 선택 효과 구현
- [x] 리액션 멤버 목록 전용 버튼 구현
- [x] 항목별 트러블슈팅 기록
- [x] 프론트와 백엔드 검증 실행

## 2026-07-10 Notice And Chat List Follow-up

- [x] INACTIVE 공지 수정 시 활성 공지 보존
- [x] 활성 공지를 작은 floating 창으로 변경
- [x] 공지 이력 날짜와 시간 표시
- [x] 권한 변경 확인 후 선택창 닫기
- [x] 방 배경 이미지 투명 효과 제거
- [x] 채팅방 사용자 리사이즈 가이드 작성
- [x] DIRECT 목록 썸네일을 상대 최신 프로필로 조회
- [x] 실시간 read와 채팅 목록 unread 동기화
- [x] 1~7번 트러블슈팅 기록
- [x] 프론트/백엔드/XML 검증

## 2026-07-10 Direct Thumbnail And Header Route Follow-up

- [x] 프로필 저장 이후 상대 브라우저의 채팅 목록 갱신 경로 확인
- [x] 폴링 없이 프로필 변경 WebSocket 이벤트 전달
- [x] DIRECT 목록만 최신 상대 프로필로 재조회
- [x] 현재 URL과 헤더 버튼 active 스타일 연결
- [x] 트러블슈팅 기록
- [x] 프론트 및 변경 파일 검증

## 2026-07-11 My Page And Join Profile Improvements

- [x] 내정보 기본 이미지 변경 기능 추가
- [x] 프로필 URL 대신 파일명 표시와 말줄임 적용
- [x] 내정보 publicId와 설명 문구 제거
- [x] 닉네임 변경과 프로필 이미지 변경 기능 분리
- [x] 헤더 프로필 이미지 클릭 시 내정보 이동
- [x] 회원가입 프로필 이미지 선택과 기본 이미지 DB 저장
- [x] 비밀번호 변경 모달과 비밀번호 확인 검증
- [x] 채팅 목록 가로 overflow 제거와 폭 조정
- [x] 트러블슈팅 기록
- [x] 프론트 및 XML 검증

## 2026-07-11 Professional Theme And AI Recommend Fix

- [x] 전체 FE 색상 토큰을 네이비·슬레이트·틸 테마로 정의
- [x] 헤더·홈·친구·채팅 목록·내정보·설정·로그인·가입 디자인 통일
- [x] ChatBox·메시지·메뉴·모달·입력 영역 디자인 통일
- [x] 기존 FE 렌더링과 이벤트 흐름 무변경 확인
- [x] 종료된 Gemini 모델 교체
- [x] AI API 키 및 공급자 오류 메시지 구체화
- [x] AI 추천 요청자의 활성 방 멤버 권한 검증
- [x] 반응형 전환 방법만 문서 코멘트로 기록
- [x] 트러블슈팅 기록
- [x] 프론트와 AI XML 검증

## 2026-07-11 Join Route And Draft Group Duplicate Creation Fix

- [x] 비로그인 상태에서도 `/join` 공개 경로 유지
- [x] 그룹 draft 첫 메시지 요청 중복 진입 차단
- [x] 전송 버튼 pending 상태 표시와 연속 클릭 차단
- [x] 기존 일반 메시지 전송 흐름 무변경 확인
- [x] 트러블슈팅 기록
- [x] 프론트 빌드와 diff 검증

## 2026-07-11 Join Profile Control And Media Type Fix

- [x] 회원가입 프로필 파일 버튼을 하나로 정리
- [x] 기본 이미지 가입은 JSON 요청으로 분리
- [x] 이미지 선택 가입은 multipart 요청 유지
- [x] backend `/user/join` JSON/multipart 계약 모두 지원
- [x] 실행 중 stale controller 확인과 기록
- [x] 트러블슈팅 기록
- [x] 프론트 빌드와 diff 검증

## 2026-07-11 Notice Width And First Message Rendering Fix

- [x] 활성 공지 bar를 채팅창 가로 폭에 맞춤
- [x] 공지 숨김/표시 toggle 위치 고정
- [x] START 응답의 firstChatMessage를 새 ChatBox 초기 데이터로 전달
- [x] Kafka DB 반영 전 HTTP 빈 조회가 초기 메시지를 지우지 않도록 병합
- [x] START broadcast와 초기 메시지 중복 렌더 방지
- [x] 트러블슈팅 기록
- [x] 프론트 빌드와 diff 검증

## 2026-07-11 Compact Layout Adjustment

- [x] 헤더 높이를 기존 대비 15% 축소
- [x] 친구 목록 메인 패널 높이 축소
- [x] 채팅방 목록을 왼쪽에 가깝게 배치하고 폭을 60% 수준으로 축소
- [x] 채팅방 목록 높이를 화면 스크롤이 덜 생기도록 축소
- [x] 채팅창 높이를 헤더와 viewport를 침범하지 않도록 축소
- [x] 프로필 원본 파일명 표시 정책 코멘트 정리
- [x] cdx0711.md 작업기록 작성
- [x] 프론트 검증 실행

## 2026-07-11 Discord Style UI Conversion (claude)

원칙 : 기능(핸들러/ws/스토어 로직/서버 호출)은 절대 건드리지 않는다. 배치와 스타일만 바꾼다. 팝아웃은 이번에 구현하지 않는다.

- [x] index.css 테마 토큰을 녹색 계열로 교체 (저채도, 가시성 우선)
- [x] chatWindowsSlice.js openChatWindow가 활성 방 1개만 유지 (디코식 싱글뷰. 팝아웃 재도입시 push 복구)
- [x] AppShell.jsx /chatList에서 좌(방 목록) + 우(활성 방) 2패널 레이아웃, ChatBox에 isDocked 전달, 빈 상태 패널
- [x] AppShell.css 2패널 레이아웃 + 빈 상태 스타일
- [x] ChatBox.jsx isDocked prop 추가 - 도킹 시 inline left/top 제거 + 드래그 비활성
- [x] ChatBox.css .docked 오버라이드 (position static, 100% 채움, 고정폭 4종 100%로)
- [x] ChatList.jsx "채팅" 버튼 제거, 행 클릭 입장, 활성 방 하이라이트 (고아가 된 버튼 CSS 제거)
- [x] ChatList.css 사이드바 형태 (전체 높이, 행 스타일, 활성 상태)
- [x] npm run build 통과 (기존 ESLint warning만 잔존)
- [x] chatUI.md G항목 결정 로그 추가
- [ ] 브라우저 시각 검증 (사용자 수행)
- [ ] 후속 : ChatBox 내부 노랑 하드코딩 색 토큰화 (도킹 검증 후)

## 2026-07-12 Discord Chat UI Regression Fixes

- [x] 입력창·첨부 버튼·전송 버튼 가로 폭 정합과 전송 버튼 10px 확대
- [x] 메시지 수와 무관하게 채팅방 상단 bar 높이 46px 고정
- [x] 리액션 변경 시 기존 스크롤 위치 보존
- [x] 리액션 표시가 발신자 프로필 위치를 밀지 않도록 메시지 행 레이아웃 수정
- [x] 답장 대상 미리보기 클릭 시 원본 메시지로 이동 및 강조
- [x] 최초 방 입장 시 최신 메시지 최하단으로 안정적으로 이동
- [x] 비활성 방 새 메시지의 채팅 목록 unread 실시간 증가 복구
- [x] 재입장 시 방 feed 이력도 메시지와 함께 복원
- [x] 우클릭 메뉴와 활성 공지를 채팅방 내부 좌표에 표시
- [x] 채팅방 메뉴를 채팅 레이아웃의 왼쪽 경계에 맞춤
- [x] cdx0712-Fe.md 작업 기록
- [x] 프론트 빌드 및 정적 검증
- [ ] `cdx0712-feed-schema.sql` DB 적용 후 다중 사용자 브라우저 검증
## 2026-07-15 AI Message Tone Refinement

- [x] 말투 다듬기 HTTP request/response 계약 추가
- [x] AI service 말투별 프롬프트와 검증 구현
- [x] ChatBox 말투 선택 UI와 현재 입력문 교체 연결
- [x] ESC 및 방 전환 시 말투 선택창 정리
- [x] ctxt.md 작업 기록
- [x] 가능한 범위의 프론트 검증
## 2026-07-15 Room Realtime And Personalized AI

- [x] 추방 확인 모달 버튼 순서와 문구 수정
- [x] 추방 대상 채팅창·채팅방 목록 실시간 제거
- [x] 영구강퇴 FE 노출 제거
- [x] 공지사항 메뉴 새 공지 작성 추가
- [x] 초대 멤버 프로필 실시간 반영
- [x] AI 처리 중 메시지 전송 차단
- [x] 말투 다듬기 적용 결과 모달과 ESC 우선순위 추가
- [x] 프로필 모달 위치·채팅방 범위 overlay 수정
- [x] 리액션 4열과 20개 선택지 추가
- [x] 검색 최신순 이동과 검색어 강조
- [x] 섬세한 맞춤 메시지 텍스트 기반 1차 버전 구현
- [x] 관련 정적 검사 수행. Gradle·Java 테스트는 프로젝트 원칙에 따라 미실행
- [x] ctxt.md 항목별 기록
