package com.chat.contract.room.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeMemberRoleCommand {
	private Long roomId;

	private Long requesterUserId;
	private String requesterPublicId;

	private String targetPublicId;
	private String targetRole;
}