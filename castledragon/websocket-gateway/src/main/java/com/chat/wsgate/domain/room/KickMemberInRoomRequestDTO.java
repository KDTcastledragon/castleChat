package com.chat.wsgate.domain.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KickMemberInRoomRequestDTO {
	private Long roomId;
	private String kickTargetPublicId;
}


