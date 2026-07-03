package com.chat.contract.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeMemberRoleInRoomRequestDTO {
	private Long roomId;
	private String targetPublicId;
}
