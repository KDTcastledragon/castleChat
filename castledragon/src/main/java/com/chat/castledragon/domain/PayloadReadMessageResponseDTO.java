package com.chat.castledragon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayloadReadMessageResponseDTO {
	private Long roomId;
	private Long lastReadMessageId;

	private String readerPublicId;
	private String readerNickname;
}