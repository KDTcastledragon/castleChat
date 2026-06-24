package com.chat.wsgate.domain;

import lombok.Data;

@Data
public class PayloadConnectUserDTO {
	private Long userId;
	private String loginId;
}
