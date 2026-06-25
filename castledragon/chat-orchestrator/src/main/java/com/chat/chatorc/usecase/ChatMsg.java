package com.chat.chatorc.usecase;

import java.util.Set;

import com.chat.chatorc.dto.PayloadReadChatMessageResponseDTO;
import com.chat.chatorc.dto.PayloadSendChatMessageRequestDTO;
import com.chat.chatorc.dto.PayloadSendChatMessageResponseDTO;

public interface ChatMsg {

	PayloadSendChatMessageResponseDTO createChatMessage(Long senderUserId, String senderPublicId, PayloadSendChatMessageRequestDTO payload, Set<Long> viewingUserIds);

	PayloadReadChatMessageResponseDTO readChatMessage(Long roomId, Long readerUserId, String readerPuublicId, Long newlastReadMessageId);

	//	Long insertMessage(Long roomId, Long senderId, String msgText);

	//	void updateLastRead(Long roomId, Long userId, Long lastReadMessageId);

}
