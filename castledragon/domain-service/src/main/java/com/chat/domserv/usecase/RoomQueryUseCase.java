package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.room.domain.ChatRoomListDTO;

public interface RoomQueryUseCase {
	List<ChatRoomListDTO> getMyAllChatRooms(Long userId);

	//	EnterRoomResponseDTO enterExistedRoom(Long roomId, SessionUserDTO me);
}
