package com.chat.wsgate.domain.chatting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadReactChatMessageRequestDTO {
	private Long roomId;
	private Long messageId;

	private String reactionType;
	private String reactionCode;

	private Boolean addRequested;
}
