package com.chat.castledragon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 내부 join용 DTO
public class ChatMemberDTO {
	private Long userId;
	private String publicId;
	private String nickname;
}