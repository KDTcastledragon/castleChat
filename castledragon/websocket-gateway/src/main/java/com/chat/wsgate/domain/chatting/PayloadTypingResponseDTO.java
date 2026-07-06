package com.chat.wsgate.domain.chatting;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayloadTypingResponseDTO {
	private Long roomId;
	private String publicId;
	private String nickname;
}


