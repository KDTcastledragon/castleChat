package com.chat.contract.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateChatMessageCommand {
	private Long roomId;
	private Long senderUserId;
	private String messageText;
	//	private String senderPublicId;
	//	private String requestId;
}
