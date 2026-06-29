package com.chat.wsgate.domain;

import lombok.Data;

@Data
public class PayloadSendChatMessageRequestDTO {
	private Long roomId;
	private String messageText;
}
