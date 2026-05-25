package com.chat.castledragon.domain;

import lombok.Data;

@Data
public class PayloadReadMessageDTO {
	private Long roomId;
	private Long userId;
	private Long lastReadMessageId;
}