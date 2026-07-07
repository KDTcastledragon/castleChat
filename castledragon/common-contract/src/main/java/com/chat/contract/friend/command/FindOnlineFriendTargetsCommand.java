package com.chat.contract.friend.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindOnlineFriendTargetsCommand {
	private Long userId;
	private String publicId;
}
