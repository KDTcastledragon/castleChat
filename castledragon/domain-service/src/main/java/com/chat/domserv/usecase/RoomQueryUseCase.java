package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.domain.ChatRoomListDTO;
import com.chat.contract.domain.EnterRoomResponseDTO;
import com.chat.contract.domain.SessionUserDTO;

public interface RoomQueryUseCase {
	List<ChatRoomListDTO> getMyAllChatRooms(Long userId);

	EnterRoomResponseDTO enterExistedRoom(Long roomId, SessionUserDTO me);
}
