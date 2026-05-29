package com.chat.castledragon.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserDTO {
	private Long userId;

	private String loginId;

	@JsonIgnore // JSON으로 응답할 때 이 필드는 빼라는 뜻이야.
	private String password;

	private String publicId;

	private String nickname;

	private String friendCode;

	private String profileImg;

	private String status;

	private LocalDateTime lastLoginedAt;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private LocalDateTime withdrawnAt;
}
