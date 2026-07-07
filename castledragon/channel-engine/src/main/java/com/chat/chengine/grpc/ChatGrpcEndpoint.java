package com.chat.chengine.grpc;

import com.chat.chengine.usecase.ChatCommandUseCase;
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
import com.chat.contract.grpc.ChEngineChatGrpc;
import com.chat.contract.grpc.CreateChatMessageRequest;
import com.chat.contract.grpc.CreateChatMessageResponse;
import com.chat.contract.grpc.DeleteChatMessageRequest;
import com.chat.contract.grpc.DeleteChatMessageResponse;
import com.chat.contract.grpc.ReactChatMessageRequest;
import com.chat.contract.grpc.ReactChatMessageResponse;
import com.chat.contract.grpc.ReadChatMessageRequest;
import com.chat.contract.grpc.ReadChatMessageResponse;
import com.chat.contract.grpc.StartDirectChatRequest;
import com.chat.contract.grpc.StartGroupChatRequest;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Log4j2
public class ChatGrpcEndpoint extends ChEngineChatGrpc.ChEngineChatImplBase {

	private final ChatCommandUseCase chatCommandUseCase;

	private CreateChatMessageResponse buildCreateChatMessageResponse(ChatMessageViewResponseDTO cmdResult) {
		CreateChatMessageResponse.Builder responseBuilder = CreateChatMessageResponse.newBuilder()
				.setMessageId(cmdResult.getMessageId())
				.setRoomId(cmdResult.getRoomId())
				.setSenderPublicId(cmdResult.getSenderPublicId())
				.setMessageType(cmdResult.getMessageType())
				.setMessageText(cmdResult.getMessageText() == null ? "" : cmdResult.getMessageText())
				.setCreatedAt(cmdResult.getCreatedAt().toString())
				.setUnreadCount(cmdResult.getUnreadCount());

		if (cmdResult.getReplyToMessageId() != null) {
			responseBuilder.setReplyToMessageId(cmdResult.getReplyToMessageId());
		}

		return responseBuilder.build();
	}

	@Override
	public void startDirectChat(StartDirectChatRequest request, StreamObserver<CreateChatMessageResponse> responseObserver) {
		StartDirectChatCommand requestedCommand = new StartDirectChatCommand(request.getTargetPublicId(), request.getSenderUserId(), request
				.getSenderPublicId(), request.getMessageType(), request
						.getMessageText(), request.hasReplyToMessageId() ? request.getReplyToMessageId() : null, request.getAttachmentIdsList());

		ChatMessageViewResponseDTO cmdResult = chatCommandUseCase.startDirectChat(requestedCommand);

		CreateChatMessageResponse response = buildCreateChatMessageResponse(cmdResult);

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void startGroupChat(StartGroupChatRequest request, StreamObserver<CreateChatMessageResponse> responseObserver) {
		StartGroupChatCommand requestedCommand = new StartGroupChatCommand(request.getRoomName(), request.getRoomThumbnail(), request
				.getInviteMemberPublicIdsList(), request.getSenderUserId(), request.getSenderPublicId(), request.getMessageType(), request
						.getMessageText(), request.hasReplyToMessageId() ? request.getReplyToMessageId() : null, request.getAttachmentIdsList());

		ChatMessageViewResponseDTO cmdResult = chatCommandUseCase.startGroupChat(requestedCommand);

		CreateChatMessageResponse response = buildCreateChatMessageResponse(cmdResult);

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void createChatMessage(CreateChatMessageRequest request, StreamObserver<CreateChatMessageResponse> responseObserver) {
		CreateChatMessageCommand requestedCommand = new CreateChatMessageCommand(request.getRoomId(), request.getSenderUserId(), request
				.getSenderPublicId(), request.getMessageType(), request
						.getMessageText(), request.hasReplyToMessageId() ? request.getReplyToMessageId() : null, request.getAttachmentIdsList());

		ChatMessageViewResponseDTO cmdResult = chatCommandUseCase.createChatMessage(requestedCommand);

		CreateChatMessageResponse response = buildCreateChatMessageResponse(cmdResult);

		responseObserver.onNext(response);
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
		DeleteChatMessageCommand requestedCommand = new DeleteChatMessageCommand(request.getRoomId(), request.getMessageId(), request
				.getRequesterUserId(), request.getRequesterPublicId());

		DeleteChatMessageResponseDTO cmdResult = chatCommandUseCase.deleteChatMessage(requestedCommand);

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
		ReactChatMessageCommand requestedCommand = new ReactChatMessageCommand(request.getRoomId(), request.getMessageId(), request
				.getRequesterUserId(), request
						.getRequesterPublicId(), request.getReactionType(), request.getReactionCode(), request.getAddRequested());

		ReactChatMessageEventResponseDTO cmdResult = chatCommandUseCase.reactChatMessage(requestedCommand);

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
