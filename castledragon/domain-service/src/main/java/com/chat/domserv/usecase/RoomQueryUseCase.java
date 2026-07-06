package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.domain.room.ChatRoomListDTO;
import com.chat.contract.domain.room.EnterRoomResponseDTO;
import com.chat.contract.domain.user.SessionUserDTO;

public interface RoomQueryUseCase {
	List<ChatRoomListDTO> getMyAllChatRooms(Long userId);

	EnterRoomResponseDTO enterExistedRoom(Long roomId, SessionUserDTO me);
}
