package com.chat.chatorc.grpc;

import com.chat.chatorc.usecase.ChatOrcCommandUseCase;
import com.chat.contract.grpc.ChatOrcGrpc;
import com.chat.contract.grpc.SendMessageRequest;
import com.chat.contract.grpc.SendMessageResponse;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

// TCP Request 받는 곳. 정확히는 gRPC 서버가 받는다.

@GrpcService
public class ChatOrcGrpcEndpoint extends ChatOrcGrpc.ChatOrcImplBase {

	private final ChatOrcCommandUseCase chatOrcCommandUseCase;

	public ChatOrcGrpcEndpoint(ChatOrcCommandUseCase chatOrcCommandUseCase) {
		this.chatOrcCommandUseCase = chatOrcCommandUseCase;
	}

	@Override
	public void sendMessage(SendMessageRequest request, StreamObserver<SendMessageResponse> responseObserver) {
		// 1. proto request -> 내부 command 변환
		// 2. usecase 호출
		// 3. 결과 -> proto response 변환

		SendMessageResponse response = SendMessageResponse.newBuilder().setMessageId(1L).setRoomId(request.getRoomId()).setSenderPublicId(request.getSenderPublicId()).setMessageText(request.getMessageText()).setCreatedAt("2026-06-26T12:00:00").setUnreadCount(0).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}