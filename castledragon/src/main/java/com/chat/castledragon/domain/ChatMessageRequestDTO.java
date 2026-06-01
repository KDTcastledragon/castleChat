package com.chat.castledragon.domain;

import lombok.Data;

@Data
public class ChatMessageRequestDTO {
	private Long roomId;

	private String msgText;
}
