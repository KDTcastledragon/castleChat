package com.chat.chengine.usecase;

import com.chat.contract.command.chatting.CreateChatMessageCommand;
import com.chat.contract.command.chatting.DeleteChatMessageCommand;
import com.chat.contract.command.chatting.ReactChatMessageCommand;
import com.chat.contract.command.chatting.ReadChatMessageCommand;
import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.DeleteChatMessageResponseDTO;
import com.chat.contract.domain.chatting.ReactChatMessageEventResponseDTO;
import com.chat.contract.domain.chatting.ReadPositionUpdateResponseDTO;

public interface ChEngineChatCommandUseCase {

	ChatMessageViewResponseDTO createChatMessage(CreateChatMessageCommand command);

	ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand command);

	DeleteChatMessageResponseDTO deleteChatMessage(DeleteChatMessageCommand command);

	ReactChatMessageEventResponseDTO reactChatMessage(ReactChatMessageCommand command);
}
