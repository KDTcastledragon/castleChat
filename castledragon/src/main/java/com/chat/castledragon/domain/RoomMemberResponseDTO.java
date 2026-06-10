package com.chat.castledragon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomMemberResponseDTO {
	private String publicId;

	private String nickname;

	private String friendCode;

	private String profileImg;

	private String role;
}