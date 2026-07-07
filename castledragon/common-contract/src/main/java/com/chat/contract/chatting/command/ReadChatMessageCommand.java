package com.chat.contract.chatting.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadChatMessageCommand {
	private Long roomId;
	private Long readerUserId;
	private String readerPublicId;
	private Long lastReadMessageId;
}
