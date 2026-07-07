package com.chat.contract.chatting.domain.res;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactChatMessageEventResponseDTO {
	private Long roomId;
	private Long messageId;

	private String requesterPublicId;

	private String reactionType;
	private String reactionCode;

	private Boolean added; // true 추가 , false 취소
	private LocalDateTime reactedAt;
}