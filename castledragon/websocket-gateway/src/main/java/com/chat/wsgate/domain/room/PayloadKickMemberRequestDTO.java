package com.chat.wsgate.domain.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayloadKickMemberRequestDTO {
	private Long roomId;
	private String kickTargetPublicId;
}


