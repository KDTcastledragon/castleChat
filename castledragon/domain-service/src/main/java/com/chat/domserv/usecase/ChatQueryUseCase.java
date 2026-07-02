package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.domain.ChatMessageViewDTO;

public interface ChatQueryUseCase {
	List<ChatMessageViewDTO> loadMessagesInRoom(Long roomId, Long beforeMessageId, int limit);
}
