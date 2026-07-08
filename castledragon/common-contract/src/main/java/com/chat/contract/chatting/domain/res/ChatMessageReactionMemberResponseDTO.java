package com.chat.contract.chatting.domain.res;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageReactionMemberResponseDTO {
	private Long roomId;
	private Long messageId;

	private String reactionType;
	private String reactionCode;

	private String requesterPublicId;
	private String requesterNickname;
	private String requesterProfileImg;

	private LocalDateTime reactedAt;
}
