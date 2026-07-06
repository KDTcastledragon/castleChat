package com.chat.contract.command.chatting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteChatMessageCommand {
	private Long roomId;
	private Long messageId;
	private Long deleterUserId;
	private String deleterPublicId;
}
