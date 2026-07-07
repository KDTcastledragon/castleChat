package com.chat.chengine.grpc;

import com.chat.chengine.usecase.ChatCommandUseCase;
import com.chat.contract.chatting.command.CreateChatMessageCommand;
import com.chat.contract.chatting.command.DeleteChatMessageCommand;
import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.command.ReadChatMessageCommand;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.chatting.domain.res.DeleteChatMessageResponseDTO;
import com.chat.contract.chatting.domain.res.ReactChatMessageEventResponseDTO;
import com.chat.contract.chatting.domain.res.ReadPositionUpdateResponseDTO;
import com.chat.contract.grpc.ChEngineChatGrpc;
import com.chat.contract.grpc.CreateChatMessageRequest;
import com.chat.contract.grpc.CreateChatMessageResponse;
import com.chat.contract.grpc.DeleteChatMessageRequest;
import com.chat.contract.grpc.DeleteChatMessageResponse;
import com.chat.contract.grpc.ReactChatMessageRequest;
import com.chat.contract.grpc.ReactChatMessageResponse;
import com.chat.contract.grpc.ReadChatMessageRequest;
import com.chat.contract.grpc.ReadChatMessageResponse;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Log4j2
public class ChEngineChatGrpcEndpoint extends ChEngineChatGrpc.ChEngineChatImplBase {

	private final ChatCommandUseCase chatCommandUseCase;

	@Override
	public void createChatMessage(CreateChatMessageRequest request, StreamObserver<CreateChatMessageResponse> responseObserver) {
		CreateChatMessageCommand requestedCommand = new CreateChatMessageCommand(request.getRoomId(), request.getSenderUserId(), request
				.getSenderPublicId(), request.getMessageType(), request
						.getMessageText(), request.hasReplyToMessageId() ? request.getReplyToMessageId() : null, request.getAttachmentIdsList());

		ChatMessageViewResponseDTO cmdResult = chatCommandUseCase.createChatMessage(requestedCommand);

		CreateChatMessageResponse.Builder responseBuilder = CreateChatMessageResponse.newBuilder()
				.setMessageId(cmdResult.getMessageId())
				.setRoomId(cmdResult.getRoomId())
				.setSenderPublicId(cmdResult.getSenderPublicId())
				.setMessageType(cmdResult.getMessageType())
				.setMessageText(cmdResult.getMessageText())
				.setCreatedAt(cmdResult.getCreatedAt().toString())
				.setUnreadCount(cmdResult.getUnreadCount());

		if (cmdResult.getReplyToMessageId() != null) {
			responseBuilder.setReplyToMessageId(cmdResult.getReplyToMessageId());
		}

		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readChatMessage(ReadChatMessageRequest request, StreamObserver<ReadChatMessageResponse> responseObserver) {
		ReadChatMessageCommand requestCommand = new ReadChatMessageCommand(request.getRoomId(), request.getReaderUserId(), request
				.getReaderPublicId(), request.getLastReadMessageId());

		log.info("gRPC EndPoint readCmd : {}", requestCommand);

		ReadPositionUpdateResponseDTO cmdResult = chatCommandUseCase.readChatMessage(requestCommand);

		ReadChatMessageResponse response = ReadChatMessageResponse.newBuilder()
				.setRoomId(cmdResult.getRoomId())
				.setReaderPublicId(cmdResult.getReaderPublicId())
				.setOldLastReadMessageId(cmdResult.getOldLastReadMessageId())
				.setLastReadMessageId(cmdResult.getLastReadMessageId())
				.setUpdated(cmdResult.getUpdated())
				.build();

		log.info("gRPC EndPoint readResponse : {}", response);

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void deleteChatMessage(DeleteChatMessageRequest request, StreamObserver<DeleteChatMessageResponse> responseObserver) {
		DeleteChatMessageCommand command = new DeleteChatMessageCommand(request.getRoomId(), request.getMessageId(), request
				.getRequesterUserId(), request.getRequesterPublicId());

		DeleteChatMessageResponseDTO cmdResult = chatCommandUseCase.deleteChatMessage(command);

		DeleteChatMessageResponse response = DeleteChatMessageResponse.newBuilder()
				.setRoomId(cmdResult.getRoomId())
				.setMessageId(cmdResult.getMessageId())
				.setRequesterPublicId(cmdResult.getRequesterPublicId())
				.setMessageStatus(cmdResult.getMessageStatus())
				.setDeletedAt(cmdResult.getDeletedAt().toString())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void reactChatMessage(ReactChatMessageRequest request, StreamObserver<ReactChatMessageResponse> responseObserver) {
		ReactChatMessageCommand command = new ReactChatMessageCommand(request.getRoomId(), request.getMessageId(), request
				.getRequesterUserId(), request
						.getRequesterPublicId(), request.getReactionType(), request.getReactionCode(), request.getAddRequested());

		ReactChatMessageEventResponseDTO cmdResult = chatCommandUseCase.reactChatMessage(command);

		ReactChatMessageResponse response = ReactChatMessageResponse.newBuilder()
				.setRoomId(cmdResult.getRoomId())
				.setMessageId(cmdResult.getMessageId())
				.setRequesterPublicId(cmdResult.getRequesterPublicId())
				.setReactionType(cmdResult.getReactionType())
				.setReactionCode(cmdResult.getReactionCode())
				.setAdded(cmdResult.getAdded())
				.setReactedAt(cmdResult.getReactedAt().toString())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
