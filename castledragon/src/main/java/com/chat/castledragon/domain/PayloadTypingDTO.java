package com.chat.castledragon.domain;

import lombok.Data;

@Data
public class PayloadTypingDTO {
	private Long roomId;
	private Long userId;
	private String loginId;
}
