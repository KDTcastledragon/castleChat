package com.chat.domserv.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
	private String loginId;

	//	@JsonIgnore // JSON으로 응답할 때 이 필드는 빼라는 뜻이야. 요청이 올때도 무시해버림;;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // 얘는 요청오는거만 허락.
	private String password;
}
