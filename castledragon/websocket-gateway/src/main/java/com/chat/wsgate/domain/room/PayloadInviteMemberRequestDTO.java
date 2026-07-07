package com.chat.wsgate.domain.room;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayloadInviteMemberRequestDTO {
	private Long roomId;
	private List<String> inviteTargetMemberPublicIds;
}

