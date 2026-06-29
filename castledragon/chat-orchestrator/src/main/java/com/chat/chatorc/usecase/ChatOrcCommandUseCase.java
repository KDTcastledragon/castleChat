package com.chat.chatorc.usecase;

import com.chat.chatorc.dto.PayloadReadChatMessageResponseDTO;
import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;

public interface ChatOrcCommandUseCase {

	ChatMessageViewDTO createChatMessage(CreateChatMessageCommand command);

	PayloadReadChatMessageResponseDTO readChatMessage(Long roomId, Long readerUserId, String readerPuublicId, Long newlastReadMessageId);

}
