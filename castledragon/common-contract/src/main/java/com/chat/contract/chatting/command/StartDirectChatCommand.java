package com.chat.contract.chatting.command;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartDirectChatCommand {
	private String targetPublicId;

	private Long senderUserId;
	private String senderPublicId;

	private String messageType;
	private String messageText;

	private Long replyToMessageId;

	private List<Long> attachmentIds;
}