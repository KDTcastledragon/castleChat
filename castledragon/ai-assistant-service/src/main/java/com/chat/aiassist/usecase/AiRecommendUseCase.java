package com.chat.aiassist.usecase;

import java.util.List;

/**
 * 메시지 추천 usecase 경계. 구현체 = AiRecommendService.
 */
public interface AiRecommendUseCase {

	List<String> recommendMessages(Long requesterUserId, Long roomId);

	List<String> recommendPersonalizedMessages(Long requesterUserId, Long roomId, String targetPublicId, String relationshipType);

	String refineMessageTone(Long requesterUserId, String messageText, String tone);

}
