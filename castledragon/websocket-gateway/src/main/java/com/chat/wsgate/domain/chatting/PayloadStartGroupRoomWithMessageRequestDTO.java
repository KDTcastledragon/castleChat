package com.chat.wsgate.domain.chatting;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadStartGroupRoomWithMessageRequestDTO {
	private String roomName;
	private String roomThumbnail;

	private List<String> inviteMemberPublicIds;

	private String messageType;
	private String messageText;

	private Long replyToMessageId;

	private List<Long> attachmentIds;
}