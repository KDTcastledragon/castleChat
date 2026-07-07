package com.chat.contract.room.command;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteMemberCommand {
	private Long roomId;

	private Long requesterUserId;
	private String requesterPublicId;

	private List<String> inviteTargetMemberPublicIds;
}