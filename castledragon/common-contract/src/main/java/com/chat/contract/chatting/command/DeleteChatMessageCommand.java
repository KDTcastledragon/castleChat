package com.chat.contract.chatting.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteChatMessageCommand {
	private Long roomId;
	private Long messageId;
	private Long requesterUserId;
	private String requesterPublicId;
}
