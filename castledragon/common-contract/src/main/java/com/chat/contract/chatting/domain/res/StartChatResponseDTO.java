package com.chat.contract.chatting.domain.res;

import com.chat.contract.room.domain.res.EnterRoomResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartChatResponseDTO {
	private EnterRoomResponseDTO enterRoomInfo;
	private ChatMessageViewResponseDTO firstChatMessage;
}