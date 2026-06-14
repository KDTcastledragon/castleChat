package com.chat.castledragon.domain;

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

	private String messageText;

	private LocalDateTime createdAt;
}
