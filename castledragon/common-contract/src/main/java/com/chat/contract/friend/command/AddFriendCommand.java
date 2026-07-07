package com.chat.contract.friend.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddFriendCommand {
	private Long requesterUserId;
	private String requesterPublicId;

	private String targetPublicId;
}