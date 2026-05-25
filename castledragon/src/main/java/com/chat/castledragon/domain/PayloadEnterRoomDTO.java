package com.chat.castledragon.domain;

import lombok.Data;

@Data
public class PayloadEnterRoomDTO {
	private Long roomId;
	private Long userId;
}