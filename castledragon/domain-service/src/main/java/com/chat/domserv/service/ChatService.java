package com.chat.domserv.service;

import java.util.List;
import java.util.Set;

import com.chat.cmctr.dto.ChatRoomListDTO;
import com.chat.cmctr.dto.EnterRoomResponseDTO;
import com.chat.cmctr.dto.SessionUserDTO;

public interface ChatService {
	EnterRoomResponseDTO getOrCreateDirectRoom(SessionUserDTO me, String friendPublicId);

	List<PayloadSendChatMessageResponseDTO> loadMessagesInRoom(Long roomId);

	//	Long insertMessage(Long roomId, Long senderId, String msgText);

	//	void updateLastRead(Long roomId, Long userId, Long lastReadMessageId);

	PayloadSendChatMessageResponseDTO createChatMessage(Long senderUserId, String senderPublicId, PayloadSendChatMessageRequestDTO payload, Set<Long> viewingUserIds);

	EnterRoomResponseDTO createGroupRoom(SessionUserDTO host, String roomName, String roomThumbnail, List<String> selectedFriendPublicIdList);

	List<ChatRoomListDTO> getMyAllChatRooms(Long userId);

	PayloadReadChatMessageResponseDTO readChatMessage(Long roomId, Long readerUserId, String readerPuublicId, Long newlastReadMessageId);

	EnterRoomResponseDTO enterExistedRoom(Long roomId, SessionUserDTO me);

	void leftRoom(Long roomId, SessionUserDTO me);

}