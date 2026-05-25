package com.chat.castledragon.domain;

import lombok.Data;

@Data
public class PayloadSendMessageDTO {
	private Long roomId;
	private Long senderId;
	private String senderLoginId;
	private String msgText;
}