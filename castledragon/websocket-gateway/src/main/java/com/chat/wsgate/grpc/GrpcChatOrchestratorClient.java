package com.chat.wsgate.grpc;

import org.springframework.stereotype.Component;

import com.chat.contract.command.SendChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.grpc.ChatOrcGrpc;
import com.chat.contract.grpc.SendMessageRequest;
import com.chat.contract.grpc.SendMessageResponse;
import com.chat.wsgate.client.ChatOrchestratorClient;

import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
public class GrpcChatOrchestratorClient implements ChatOrchestratorClient {

	@GrpcClient("chat-orchestrator")
	private ChatOrcGrpc.ChatOrcBlockingStub chatOrcStub;

	@Override
	public ChatMessageViewDTO sendMessage(SendChatMessageCommand command) {
		SendMessageRequest request = SendMessageRequest.newBuilder().setRoomId(command.getRoomId()).setSenderUserId(command.getSenderUserId()).setSenderPublicId(command.getSenderPublicId()).setMessageText(command.getMessageText()).addAllViewingUserIds(command.getViewingUserIds()).setRequestId(command.getRequestId()).build();

		SendMessageResponse response = chatOrcStub.sendMessage(request);

		return new ChatMessageViewDTO(response.getMessageId(), response.getRoomId(), response.getSenderPublicId(), response.getMessageText(), response.getCreatedAt(), response.getUnreadCount());
	}
}