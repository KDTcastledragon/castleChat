package com.chat.wsgate.domain.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadRespondFriendRequestDTO {
	private String requesterPublicId;
	private String friendAction; // ACCEPT / REJECT
}