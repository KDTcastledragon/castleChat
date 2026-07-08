package com.chat.contract.chatting.domain.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageReactionSummaryDTO {
	private Long messageId;
	private String reactionType;
	private String reactionCode;
	private Long count;
}
