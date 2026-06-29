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
	private String senderPublicId;
	private String messageText;
	//	private String requestId; // broad가 해준다. 필요없음.
}
