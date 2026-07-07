package com.chat.contract.chatting.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatMessagesDTO {
	private Long messageId;

	private Long roomId;
	private Long senderId;
	private String messageType;

	private String messageText;

	private Long replyToMessageId;

	private String messageStatus;

	private LocalDateTime createdAt;
	private LocalDateTime deletedAt;
}
