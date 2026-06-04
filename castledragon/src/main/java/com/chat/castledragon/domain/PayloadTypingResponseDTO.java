package com.chat.castledragon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayloadTypingResponseDTO {
	private Long roomId;
	private String publicId;
	private String nickname;
}
