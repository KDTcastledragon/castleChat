package com.chat.aiassist.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.chat.aiassist.client.GeminiClient;
import com.chat.aiassist.domain.RecentMessageDTO;
import com.chat.aiassist.mapper.AiAssistMapper;
// import com.chat.aiassist.support.AiRecommendRateLimiter;
import com.chat.aiassist.usecase.AiRecommendUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 메시지 추천 본체.
 *
 * 흐름 : 최근 메시지 select(read-only) -> 프롬프트 조립 -> GeminiClient 호출 -> 추천 목록 파싱. TODO(완성본) : 성향 프로필 캐싱 결합 / rate limit 복구 / 결과 캐싱. (aiAssistant.md E항목)
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class AiRecommendService implements AiRecommendUseCase {

	private final AiAssistMapper aiAssistMapper;
	private final GeminiClient geminiClient;
	// 테스트 기간에는 무료키 사용량 제한 없이 검증하기 위해 rate limit 호출을 막아둔다.
	// private final AiRecommendRateLimiter aiRecommendRateLimiter;

	private static final int RECENT_MESSAGE_LIMIT = 30;
	private static final int RECOMMEND_COUNT = 3;

	@Override
	public List<String> recommendMessages(Long requesterUserId, Long roomId) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		List<RecentMessageDTO> recentMessages = aiAssistMapper.findRecentMessagesInRoom(roomId, RECENT_MESSAGE_LIMIT);

		if (recentMessages == null || recentMessages.isEmpty()) {
			return List.of("대화 내용이 없어 추천할 수 없습니다.");
		}

		// 테스트 기간에는 무료키 사용량 제한 없이 검증한다.
		// 완성본에서는 아래 호출을 복구한다.
		// aiRecommendRateLimiter.checkRecommendLimit(requesterUserId);

		// 최신순으로 뽑았으니 시간순으로 뒤집어서 프롬프트에 넣는다.
		Collections.reverse(recentMessages);

		String prompt = buildRecommendPrompt(recentMessages);
		String generated = geminiClient.generateText(prompt);

		List<String> recommendations = parseRecommendations(generated);
		log.info("추천 완료. room={}, 추천수={}", roomId, recommendations.size());

		return recommendations;
	}

	private String buildRecommendPrompt(List<RecentMessageDTO> recentMessages) {
		StringBuilder conversation = new StringBuilder();

		for (RecentMessageDTO msg : recentMessages) {
			conversation.append(msg.getSenderNickname()).append(" : ").append(msg.getMessageText()).append("\n");
		}

		return """
				다음은 채팅방의 최근 대화다. 대화의 맥락과 상대의 말투/성향을 파악해서,
				지금 시점에 보내기 가장 좋은 메시지를 %d개 추천해라.

				규칙:
				- 각 추천 메시지는 한 줄로, 줄바꿈으로만 구분해서 출력해라.
				- 번호, 따옴표, 설명, 마크다운 없이 "보낼 메시지 원문"만 출력해라.
				- 대화에서 쓰인 언어와 말투를 그대로 따라라.

				[대화]
				%s""".formatted(RECOMMEND_COUNT, conversation);
	}

	private List<String> parseRecommendations(String generated) {
		if (generated == null || generated.isBlank()) {
			return List.of("추천 생성에 실패했습니다.");
		}

		List<String> recommendations = new ArrayList<>();

		for (String line : Arrays.asList(generated.split("\n"))) {
			String trimmed = line.trim();

			if (!trimmed.isEmpty()) {
				recommendations.add(trimmed);
			}

			if (recommendations.size() >= RECOMMEND_COUNT) {
				break;
			}
		}

		return recommendations.isEmpty() ? List.of("추천 생성에 실패했습니다.") : recommendations;
	}

}
