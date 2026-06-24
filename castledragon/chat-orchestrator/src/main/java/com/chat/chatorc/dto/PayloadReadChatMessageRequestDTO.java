package com.chat.chatorc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayloadReadChatMessageRequestDTO {
	private Long roomId;
	private Long lastReadMessageId;
}