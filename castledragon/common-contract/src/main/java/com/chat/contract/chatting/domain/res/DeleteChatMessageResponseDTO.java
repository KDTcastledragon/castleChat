package com.chat.contract.chatting.domain.res;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteChatMessageResponseDTO {
	private Long roomId;
	private Long messageId;

	private String requesterPublicId;

	private String messageStatus;
	private LocalDateTime deletedAt;
}
