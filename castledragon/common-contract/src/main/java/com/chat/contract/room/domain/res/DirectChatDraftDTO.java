package com.chat.contract.room.domain.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirectChatDraftDTO {
	private String friendPublicId;
	private String friendNickname;
	private String friendProfileImg;
}
