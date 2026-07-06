package com.chat.wsgate.domain.chatting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadDeleteChatMessageRequestDTO {
	private Long roomId;
	private Long messageId;
}

