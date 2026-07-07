package com.chat.wsgate.client;

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

public interface WsGateChEngineChatClient {
	ChatMessageViewResponseDTO startDirectChat(StartDirectChatCommand command);

	ChatMessageViewResponseDTO startGroupChat(StartGroupChatCommand command);

	ChatMessageViewResponseDTO createChatMessage(CreateChatMessageCommand command);

	ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand command);

	DeleteChatMessageResponseDTO deleteChatMessage(DeleteChatMessageCommand command);

	ReactChatMessageEventResponseDTO reactChatMessage(ReactChatMessageCommand command);

}
