package com.chat.wsgate.client;

import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.command.ReadChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ReadPositionUpdateResponseDTO;

public interface WsGateChatOrchestratorClient {
	ChatMessageViewDTO createChatMessage(CreateChatMessageCommand command);

	ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand command);
}
