package com.chat.castledragon.service;

import java.util.List;
import java.util.Set;

import com.chat.castledragon.domain.PayloadSendChatMessageResponseDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.EnterRoomResponseDTO;
import com.chat.castledragon.domain.PayloadReadChatMessageResponseDTO;
import com.chat.castledragon.domain.PayloadSendChatMessageRequestDTO;
import com.chat.castledragon.domain.SessionUserDTO;

public interface ChatService {
	EnterRoomResponseDTO getOrCreateDirectRoom(SessionUserDTO me, String friendPublicId);

	List<PayloadSendChatMessageResponseDTO> loadMessagesInRoom(Long roomId);

	//	Long insertMessage(Long roomId, Long senderId, String msgText);

	//	void updateLastRead(Long roomId, Long userId, Long lastReadMessageId);

	PayloadSendChatMessageResponseDTO createChatMessage(Long senderUserId, String senderPublicId, PayloadSendChatMessageRequestDTO payload, Set<Long> viewingUserIds);

	EnterRoomResponseDTO createGroupRoom(SessionUserDTO host, String roomName, String roomThumbnail, List<String> selectedFriendPublicIdList);

	List<ChatRoomListDTO> getMyAllRooms(Long userId);

	PayloadReadChatMessageResponseDTO readChatMessage(Long roomId, Long readerUserId, String readerPuublicId, Long newlastReadMessageId);

}