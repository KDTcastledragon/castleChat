package com.chat.contract.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadPositionUpdateResponseDTO {
	private Long roomId;
	private String readerPublicId;
	private Long oldLastReadMessageId;
	private Long lastReadMessageId;
	private Boolean updated;
}
