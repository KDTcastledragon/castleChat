package com.chat.aiassist.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiClientTest {

	private static final String REQUEST_URL = "https://example.test/v1beta/models/test-model:generateContent";

	private GeminiClient geminiClient;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test/v1beta");
		server = MockRestServiceServer.bindTo(builder).build();

		geminiClient = new GeminiClient("test-key", "test-model");
		ReflectionTestUtils.setField(geminiClient, "restClient", builder.build());
	}

	@Test
	void retriesServiceUnavailableAndReturnsNextSuccessfulResponse() {
		server.expect(once(), requestTo(REQUEST_URL))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
						.contentType(MediaType.APPLICATION_JSON)
						.body("""
								{"error":{"code":503,"status":"UNAVAILABLE"}}
								"""));
		server.expect(once(), requestTo(REQUEST_URL))
				.andRespond(withSuccess("""
						{"candidates":[{"content":{"parts":[{"text":"추천 메시지"}]}}]}
						""", MediaType.APPLICATION_JSON));

		String result = geminiClient.generateText("대화 내용");

		assertThat(result).isEqualTo("추천 메시지");
		server.verify();
	}

	@Test
	void failsWithFriendlyMessageAfterThreeServiceUnavailableResponses() {
		server.expect(times(3), requestTo(REQUEST_URL))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
						.contentType(MediaType.APPLICATION_JSON)
						.body("""
								{"error":{"code":503,"status":"UNAVAILABLE"}}
								"""));

		assertThatThrownBy(() -> geminiClient.generateText("대화 내용"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("AI 모델이 혼잡합니다. 잠시 후 다시 시도해 주세요.");
		server.verify();
	}
}
