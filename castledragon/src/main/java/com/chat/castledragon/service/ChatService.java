package com.chat.castledragon.service;

import java.util.List;
import java.util.Set;

import com.chat.castledragon.domain.ChatDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.EnterRoomResponseDTO;
import com.chat.castledragon.domain.PayloadSendMessageDTO;

public interface ChatService {
	EnterRoomResponseDTO enterRoom(Long senderId, Long targetUserId);

	List<ChatDTO> getMessages(Long roomId);

	Long insertMessage(Long roomId, Long senderId, String msgText);

	void updateLastRead(Long roomId, Long userId, Long lastReadMessageId);

	List<ChatRoomListDTO> getMyChatRooms(Long userId);

	ChatDTO sendMessage(PayloadSendMessageDTO payload, Set<Long> viewingUserIds);
}
