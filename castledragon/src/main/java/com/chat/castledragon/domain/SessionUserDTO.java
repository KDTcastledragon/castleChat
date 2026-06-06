package com.chat.castledragon.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

// 서버가 요청자를 식별하고 인가하기 위한 최소 정보
public class SessionUserDTO implements Serializable {
	// Java 직렬화에서 “이 클래스 버전 번호” 같은 거야. Spring Session Redis가 session 객체를 저장할 때 직렬화할 수 있어서 SessionUserDTO implements Serializable이면 붙는 경우가 많아.
	// 세션에 저장되는 객체니까 Serializable 필요할 수 있음. serialVersionUID는 그 직렬화 버전 표시
	private static final long serialVersionUID = 1L;

	private Long userId;

	private String publicId;

	private String nickname;

	private String friendCode;

	private String profileImg;
}
