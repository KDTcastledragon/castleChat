package com.chat.castledragon.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

// 서버가 요청자를 식별하고 인가하기 위한 최소 정보
public class SessionUserDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long userId;

	private String publicId;

	private String nickname;

	private String profileImg;
}
