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

	private String roomType;

	private String customRoomName;
	private String customRoomThumbnail;

	private Long roomMemberCount;

	private List<RoomMemberResponseDTO> memberList;

	private Long lastReadMessageId;
}