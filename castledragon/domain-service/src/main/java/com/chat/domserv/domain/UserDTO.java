package com.chat.domserv.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserDTO {
	private Long userId;

	private String loginId;

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
