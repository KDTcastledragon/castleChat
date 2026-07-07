package com.chat.contract.friend.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespondFriendCommand {
	private Long responderUserId;
	private String responderPublicId;

	private String requesterPublicId;
	private String friendAction; // ACCEPT / REJECT
}