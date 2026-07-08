package com.chat.chengine.usecase;

import com.chat.contract.chatting.command.CreateChatMessageCommand;
import com.chat.contract.chatting.command.DeleteChatMessageCommand;
import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.command.ReadChatMessageCommand;
import com.chat.contract.chatting.command.StartDirectChatCommand;
import com.chat.contract.chatting.command.StartGroupChatCommand;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.chatting.domain.res.DeleteChatMessageResponseDTO;
import com.chat.contract.chatting.domain.res.ReactChatMessageEventResponseDTO;
import com.chat.contract.chatting.domain.res.ReadPositionUpdateResponseDTO;
import com.chat.contract.chatting.domain.res.StartChatResponseDTO;

public interface ChatCommandUseCase {
	StartChatResponseDTO startDirectChat(StartDirectChatCommand command);

	StartChatResponseDTO startGroupChat(StartGroupChatCommand command);

	ChatMessageViewResponseDTO createChatMessage(CreateChatMessageCommand command);

	ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand command);

	DeleteChatMessageResponseDTO deleteChatMessage(DeleteChatMessageCommand command);

	ReactChatMessageEventResponseDTO reactChatMessage(ReactChatMessageCommand command);
}
