package com.chat.contract.command.chatting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactChatMessageCommand {
	private Long roomId;
	private Long messageId;
	private Long reactorUserId;
	private String reactorPublicId;
	private String reactionType;
	private String reactionCode;

	private Boolean addRequested;
}
