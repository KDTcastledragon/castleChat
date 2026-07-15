package com.chat.aiassist.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import lombok.extern.log4j.Log4j2;

/**
 * Gemini API HTTP 텍스트 클라이언트.
 *
 * 현재 AI Assist는 LLM 텍스트 추천만 사용한다.
 * 무료 티어 rate limit(분당/일일 상한)이 있으므로, AiRecommendRateLimiter + 결과 캐싱이 이 클래스 앞단에 필요하다.
 */
@Component
@Log4j2
public class GeminiClient {
	private static final int MAX_ATTEMPTS = 3;
	private static final long INITIAL_RETRY_DELAY_MS = 1000L;

	private final RestClient restClient;
	private final String model;
	private final boolean apiKeyConfigured;

	public GeminiClient(@Value("${ai.gemini.api-key:}") String apiKey, @Value("${ai.gemini.model:gemini-3.5-flash}") String model) {
		this.model = model;
		this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();
		this.restClient = RestClient.builder()
				.baseUrl("https://generativelanguage.googleapis.com/v1beta")
				.defaultHeader("x-goog-api-key", apiKey)
				.build();
	}

	/**
	 * 텍스트 프롬프트 -> Gemini 응답 텍스트.
	 * 실패 시 IllegalStateException (호출부에서 UX에 맞게 처리)
	 */
	@SuppressWarnings("unchecked")
	public String generateText(String prompt) {
		if (!apiKeyConfigured) {
			throw new IllegalStateException("Gemini API key가 설정되지 않았습니다.");
		}

		Map<String, Object> requestBody = Map.of(
				"contents", List.of(Map.of(
						"parts", List.of(Map.of("text", prompt)))));

		try {
			Map<String, Object> response = requestGenerateContent(requestBody);

			// 응답 구조 : candidates[0].content.parts[0].text
			List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

			if (candidates == null || candidates.isEmpty()) {
				throw new IllegalStateException("Gemini 응답에 candidates 없음");
			}

			Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
			List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

			return (String) parts.get(0).get("text");
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			log.error("Gemini generateContent 호출 실패. model={}", model, e);
			throw new IllegalStateException("Gemini 호출 실패");
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> requestGenerateContent(Map<String, Object> requestBody) {
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				return restClient.post()
						.uri("/models/{model}:generateContent", model)
						.body(requestBody)
						.retrieve()
						.body(Map.class);
			} catch (RestClientResponseException e) {
				if (e.getStatusCode().value() != 503) {
					log.error("Gemini generateContent HTTP 실패. model={}, status={}, body={}", model, e.getStatusCode(), e
							.getResponseBodyAsString());
					throw new IllegalStateException("Gemini API 요청 실패. status=" + e.getStatusCode().value());
				}

				if (attempt == MAX_ATTEMPTS) {
					log.error("Gemini generateContent 503 반복 실패. model={}, attempts={}", model, MAX_ATTEMPTS);
					throw new IllegalStateException("AI 모델이 혼잡합니다. 잠시 후 다시 시도해 주세요.");
				}

				long retryDelayMs = INITIAL_RETRY_DELAY_MS * (1L << (attempt - 1));
				log.warn("Gemini generateContent 503 재시도. model={}, attempt={}, delayMs={}", model, attempt, retryDelayMs);
				waitBeforeRetry(retryDelayMs);
			}
		}

		throw new IllegalStateException("Gemini 호출 실패");
	}

	private void waitBeforeRetry(long retryDelayMs) {
		try {
			Thread.sleep(retryDelayMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Gemini 재시도 대기 중 요청이 중단되었습니다.");
		}
	}

}
