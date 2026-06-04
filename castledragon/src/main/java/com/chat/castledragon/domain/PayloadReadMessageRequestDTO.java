package com.chat.castledragon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayloadReadMessageRequestDTO {
	private Long roomId;
	private Long lastReadMessageId;

}