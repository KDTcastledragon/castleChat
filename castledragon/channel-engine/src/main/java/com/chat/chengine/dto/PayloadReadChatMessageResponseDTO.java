package com.chat.chengine.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayloadReadChatMessageResponseDTO {
	private Long roomId;
	private String readerPublicId;
	private Long updatedLastReadMessageId;

	private List<UpdatedUnreadMessagesDTO> updatedMessages;
}

