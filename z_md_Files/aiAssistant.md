< AI 어시스턴트 설계 문서 (ai-assistant-service) >

목적 : 채팅상대 성향 파악 + 현 시점 가장 보내기 좋은 추천 메시지 목록 제공. Gemini API 사용(무료 티어, LLM 텍스트 호출만 사용).

prpr 준수 검증:
- prpr 1 (대용량 트래픽) : AI 호출(초 단위 지연)을 별도 프로세스로 격리 -> cheg/wsgate hot path에 영향 0.
  프로필 캐싱으로 "요청마다 전체 히스토리 분석" 금지. rate limit 대비 호출 큐잉 설계.
- prpr 2 (Client UX) : 추천 요청은 HTTP 비동기(fe에서 로딩표시)라 채팅 UX와 분리됨.
- prpr 5 (naming) : 기존 스타일 유지 (패키지 com.chat.aiassist / 클래스 AiRecommendService 등).

===========================================================================================

A. 아키텍처 결정사항

1. 별도 프로세스 (ai-assistant-service.jar, port 9200) : AI 지연/장애가 채팅 코어에 못 번지게 격리.
2. HTTP 기반 (WS 아님) : 추천은 "요청->응답" 패턴. 실시간 스트림이 아님.
3. VLM 도입 안 함 : 현재 AI Assist는 Gemini API의 텍스트 LLM 호출만 사용한다.
   이미지/영상 이해, 캡셔닝, 멀티모달 분석은 포트폴리오 비용/복잡도 때문에 제외한다.
4. 성향 프로필은 캐싱 : 매 요청 전체 히스토리 분석 금지. 주기적/증분 생성해서 저장해두고,
   추천 시엔 "프로필 + 최근 메시지 N개"만 프롬프트에 넣음.
5. 인증 : spring-session redis 공유(namespace castlechat:session 동일) -> 기존 로그인 세션 그대로 사용.
   별도 인증 체계 불필요.

B. 데이터 흐름

[추천 요청 (동기 HTTP 호출 체인)]
fe --HTTP--> aiassist.AiRecommendController (/aiRecommend/recommendMessages/{roomId})
  -> AiRecommendUseCase -> AiRecommendService
     (1) 상대 성향 프로필 조회 (캐시 hit -> 바로 사용 / miss -> 생성 후 저장)
     (2) 최근 텍스트 메시지 N개 select
     (3) 프롬프트 조립 -> GeminiClient 호출 -> 추천 목록 파싱
  -> 추천 메시지 목록 응답

C. Gemini 무료 티어 rate limit 전략

- 공식 기준 : Gemini API rate limit은 RPM/TPM/RPD 기준이며, "API key 단위"가 아니라 "Google Cloud project 단위"로 적용된다.
  실제 active limit은 모델/계정/티어에 따라 변하고 AI Studio에서 확인해야 한다.
- 현재 프로젝트는 기업 서비스가 아니라 취업준비생 포트폴리오이므로, 무료키(castleApiKey)를 많은 유저가 자유롭게 쓰게 만들면 안 된다.
- 대응 : Redis 기반 rate limit을 둔다. JVM 메모리 제한이 아니라 Redis를 쓰는 이유는 ai-assistant-service가 여러 인스턴스로 늘어나도 제한이 공유되어야 하기 때문.
- 현재 테스트 상태 : Gemini 실제 연동 검증을 위해 rate limit 호출부와 application.properties 제한값을 주석 처리해두었다.
  테스트 완료 후 아래 제한값을 복구한다.
- 완성본 설정값(application.properties 기준)
  1. user-per-minute = 1  : 한 유저는 1분에 AI 추천 1회만 가능.
  2. user-per-day = 5     : 한 유저는 하루에 AI 추천 5회만 가능.
  3. global-per-minute = 5: 전체 서비스에서 1분에 5회만 Gemini 호출 가능.
  4. global-per-day = 80  : 전체 서비스에서 하루 80회까지만 Gemini 호출 가능.
- 수치 결정 이유 : 포트폴리오 시연에는 충분하지만, 여러 유저가 반복 클릭해도 무료 Gemini project quota를 빨리 태우지 않게 하는 보수적 제한이다.
- 초과 시 HTTP 429(TOO_MANY_REQUESTS)로 응답한다.
- 완성본에서는 (1) 추천 결과 단기 캐싱 (2) 성향 프로필 장기 캐싱 (3) 유료 전환 또는 Groq/OpenRouter fallback을 추가한다.

D. 현재 뼈대 구성 (2026-07-09 생성)

castledragon/ai-assistant-service/
├── build.gradle                  : web + mybatis/mariadb + redis세션공유 + spring-kafka + actuator
├── .project / .classpath / .settings : Eclipse(Buildship) 메타. 기존 모듈과 동일 패턴.
└── src/main/
    ├── java/com/chat/aiassist/
    │   ├── AiAssistantServiceApplication.java   : 메인
    │   ├── controller/AiRecommendController.java: HTTP 진입점 (/aiRecommend/ping, 추천 API)
    │   ├── usecase/AiRecommendUseCase.java      : 경계 인터페이스
    │   ├── service/AiRecommendService.java      : 추천 본체
    │   ├── support/AiRecommendRateLimiter.java  : Redis 기반 user/global rate limit
    │   ├── client/GeminiClient.java             : Gemini HTTP 텍스트 클라이언트
    │   ├── exception/*                          : 429/400/502 응답 처리
    │   └── worker/AttachmentCaptionWorker.java  : 현재 미사용. VLM/캡셔닝 재도입시 연결
    └── resources/application.properties         : port 9200, 세션공유, kafka, gemini 설정

- jar 빌드 확인됨 : gradlew :ai-assistant-service:build -> build/libs/ai-assistant-service-0.0.1-SNAPSHOT.jar
- castleApiKey는 환경변수로 주입 (properties에 키 직접 기재 금지. git 커밋 금지.)
  fallback으로 GEMINI_API_KEY도 허용한다.

E. 다음 구현 순서 (제안)

1. 추천 파이프라인 실기동 검증 : 최근 메시지 select -> 프롬프트 -> Gemini -> 추천 목록 응답
2. 성향 프로필 생성/캐싱 + 추천 프롬프트에 결합
3. 결과 단기 캐싱 (같은 방에서 반복 클릭시 Gemini 재호출 방지)
4. rate limit 초과시 fe UI 처리 (429 메시지 표시)
5. fe UI (추천 목록 표시 + 클릭시 입력창 삽입)
