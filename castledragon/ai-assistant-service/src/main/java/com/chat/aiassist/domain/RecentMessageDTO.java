package com.chat.aiassist.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 추천 프롬프트에 넣을 최근 메시지 한 건.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentMessageDTO {
	private Long messageId;
	private Long senderId;
	private String senderNickname;
	private String messageText;
	private LocalDateTime createdAt;
}
