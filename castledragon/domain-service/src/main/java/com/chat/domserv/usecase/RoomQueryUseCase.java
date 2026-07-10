package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.room.domain.ChatRoomListDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewDTO;

public interface RoomQueryUseCase {
	List<ChatRoomListDTO> getMyAllChatRooms(Long userId);

	List<RoomNoticeViewDTO> loadRoomNotices(Long roomId, Long beforeRoomNoticeId, int limit, Long requesterUserId);

	//	EnterRoomResponseDTO enterExistedRoom(Long roomId, SessionUserDTO me);
}
