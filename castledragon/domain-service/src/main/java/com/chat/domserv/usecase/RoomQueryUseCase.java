package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.room.domain.ChatRoomListDTO;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.user.domain.SessionUserDTO;

public interface RoomQueryUseCase {
	List<ChatRoomListDTO> getMyAllChatRooms(Long userId);

	EnterRoomResponseDTO enterExistedRoom(Long roomId, SessionUserDTO me);
}
