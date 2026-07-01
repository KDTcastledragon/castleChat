package com.chat.chatorc.usecase;

import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.command.ReadChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ReadPositionUpdateResponseDTO;

public interface ChatOrcCommandUseCase {

	ChatMessageViewDTO createChatMessage(CreateChatMessageCommand command);

	ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand command);

}
