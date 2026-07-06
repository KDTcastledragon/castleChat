package com.chat.contract.domain.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDTO {
	private String publicId;

	private String nickname;

	private String friendCode;

	private String profileImg;
}