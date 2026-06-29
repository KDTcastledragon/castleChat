package com.chat.wsgate.client;

import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;

public interface ChatOrchestratorClient {
	ChatMessageViewDTO createChatMessage(CreateChatMessageCommand command);
}
