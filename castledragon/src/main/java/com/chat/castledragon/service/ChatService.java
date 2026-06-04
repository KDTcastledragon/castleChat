package com.chat.castledragon.service;

import java.util.List;
import java.util.Set;

import com.chat.castledragon.domain.ChatMessageResponseDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.EnterRoomResponseDTO;
import com.chat.castledragon.domain.PayloadSendMessageDTO;

public interface ChatService {
	EnterRoomResponseDTO enterDirectRoom(Long senderId, String friendPublicId);

	List<ChatMessageResponseDTO> getPrevMessagesInRoom(Long roomId);

	//	Long insertMessage(Long roomId, Long senderId, String msgText);

	void updateLastRead(Long roomId, Long userId, Long lastReadMessageId);

	List<ChatRoomListDTO> getMyChatRooms(Long userId);

	ChatMessageResponseDTO sendMessage(Long senderUserId, String senderPublicId, PayloadSendMessageDTO payload, Set<Long> viewingUserIds);
}
