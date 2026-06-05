package com.chat.castledragon.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnterGroupResponseDTO {
	private Long roomId;
	private String roomName;
	private Long roomMemberCount;
	private List<UserProfileResponseDTO> memberList;
}
