package com.chat.castledragon.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EnterRoomResponseDTO {

	private Long roomId;

	private Long targetUserId;

	private String targetLoginId;

	private List<ChatMessageDTO> messages;

	private Long lastReadMessageId;
}