package com.chat.contract.room.domain.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenDirectChatRoomResponseDTO {
	private Boolean roomExists;
	private EnterRoomResponseDTO enterRoomInfo;
	private DirectChatDraftDTO draft;
}
