package com.chat.wsgate.domain.chatting;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadSendChatMessageRequestDTO {
	private Long roomId;
	private String messageType;
	private String messageText;
	private Long replyToMessageId;
	private List<Long> attachmentIds;
}


