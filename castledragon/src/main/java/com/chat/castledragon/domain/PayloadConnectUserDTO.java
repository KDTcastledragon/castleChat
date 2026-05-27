package com.chat.castledragon.domain;

import lombok.Data;

@Data
public class PayloadConnectUserDTO {
	private Long userId;
	private String loginId;
}
