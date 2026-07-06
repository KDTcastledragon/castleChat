package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;

public interface ChatQueryUseCase {
	List<ChatMessageViewResponseDTO> loadMessagesInRoom(Long roomId, Long beforeMessageId, int limit);
}
