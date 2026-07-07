package com.chat.wsgate.domain.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadBanMemberRequestDTO {
	private Long roomId;
	private String banTargetPublicId;
}
