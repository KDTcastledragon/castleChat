package com.chat.wsgate.domain.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadAddFriendRequestDTO {
	private String targetPublicId;
}