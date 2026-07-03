package com.chat.contract.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteMemberInRoomRequestDTO {
	private Long roomId;
	private List<String> inviteMemberPublicIds;
}