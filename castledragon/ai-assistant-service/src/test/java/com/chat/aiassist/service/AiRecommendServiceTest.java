package com.chat.aiassist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chat.aiassist.client.GeminiClient;
import com.chat.aiassist.domain.RecentMessageDTO;
import com.chat.aiassist.mapper.AiAssistMapper;

@ExtendWith(MockitoExtension.class)
class AiRecommendServiceTest {

	@Mock
	private AiAssistMapper aiAssistMapper;

	@Mock
	private GeminiClient geminiClient;

	@InjectMocks
	private AiRecommendService aiRecommendService;

	@Test
	void refineMessageToneUsesCurrentDraftAndSelectedTone() {
		when(geminiClient.generateText(anyString())).thenReturn("오늘 회의 참석이 어렵습니다. 진행 내용 공유 부탁드립니다.");

		String refinedMessage = aiRecommendService.refineMessageTone(1L, "야 오늘 회의 못 갈 듯", "POLITE");

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(geminiClient).generateText(promptCaptor.capture());

		assertEquals("오늘 회의 참석이 어렵습니다. 진행 내용 공유 부탁드립니다.", refinedMessage);
		assertTrue(promptCaptor.getValue().contains("정중하고 예의 있게"));
		assertTrue(promptCaptor.getValue().contains("야 오늘 회의 못 갈 듯"));
	}

	@Test
	void personalizedRecommendKeepsRequesterPerspectiveAndGroupContext() {
		RecentMessageDTO mine = new RecentMessageDTO(1L, 10L, "나", "오늘 저녁 어때?", LocalDateTime.of(2026, 7, 15, 18, 0));
		RecentMessageDTO target = new RecentMessageDTO(2L, 20L, "상대", "다 같이 보는 영화는 어때?", LocalDateTime.of(2026, 7, 15, 18, 1));

		when(aiAssistMapper.findUserIdByPublicId("target-public-id")).thenReturn(20L);
		when(aiAssistMapper.countActiveRoomMember(30L, 10L)).thenReturn(1);
		when(aiAssistMapper.countActiveRoomMember(30L, 20L)).thenReturn(1);
		when(aiAssistMapper.findSharedConversationMessages(eq(10L), eq(20L), eq(180))).thenReturn(List.of(target, mine));
		when(aiAssistMapper.findRecentMessagesInRoom(30L, 30)).thenReturn(List.of(target, mine));
		when(aiAssistMapper.findRoomType(30L)).thenReturn("GROUP");
		when(geminiClient.generateText(anyString())).thenReturn("그 영화 다 같이 보러 갈래?\n시간 맞춰보자\n다른 사람들 의견도 물어보자");

		List<String> result = aiRecommendService.recommendPersonalizedMessages(10L, 30L, "target-public-id", "FLIRTING");

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(geminiClient).generateText(promptCaptor.capture());

		assertEquals(3, result.size());
		assertTrue(promptCaptor.getValue().contains("현재 방이 GROUP이면"));
		assertTrue(promptCaptor.getValue().contains("[요청자/나]"));
		assertTrue(promptCaptor.getValue().contains("[선택 상대/상대]"));
	}
}
