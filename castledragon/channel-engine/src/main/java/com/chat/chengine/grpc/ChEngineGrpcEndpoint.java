package com.chat.chengine.grpc;

import com.chat.chengine.usecase.ChEngineChatCommandUseCase;
import com.chat.contract.command.chatting.CreateChatMessageCommand;
import com.chat.contract.command.chatting.DeleteChatMessageCommand;
import com.chat.contract.command.chatting.ReactChatMessageCommand;
import com.chat.contract.command.chatting.ReadChatMessageCommand;
import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.DeleteChatMessageResponseDTO;
import com.chat.contract.domain.chatting.ReactChatMessageEventResponseDTO;
import com.chat.contract.domain.chatting.ReadPositionUpdateResponseDTO;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;
import com.chat.contract.grpc.ApplyRoomNoticeRequest;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.ChEngineGrpc;
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
public class ChEngineGrpcEndpoint extends ChEngineGrpc.ChEngineImplBase {

	private final ChEngineChatCommandUseCase chEngineChatCommandUseCase;

	@Override
	public void createChatMessage(CreateChatMessageRequest request, StreamObserver<CreateChatMessageResponse> responseObserver) {
		CreateChatMessageCommand requestedCommand = new CreateChatMessageCommand(request.getRoomId(), request.getSenderUserId(), request
				.getSenderPublicId(), request.getMessageType(), request
						.getMessageText(), request.hasReplyToMessageId() ? request.getReplyToMessageId() : null, request.getAttachmentIdsList());

		ChatMessageViewResponseDTO cmdResult = chEngineChatCommandUseCase.createChatMessage(requestedCommand);

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

		ReadPositionUpdateResponseDTO cmdResult = chEngineChatCommandUseCase.readChatMessage(requestCommand);

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
				.getDeleterUserId(), request.getDeleterPublicId());

		DeleteChatMessageResponseDTO result = chEngineChatCommandUseCase.deleteChatMessage(command);

		DeleteChatMessageResponse response = DeleteChatMessageResponse.newBuilder()
				.setRoomId(result.getRoomId())
				.setMessageId(result.getMessageId())
				.setDeleterPublicId(result.getDeleterPublicId())
				.setMessageStatus(result.getMessageStatus())
				.setDeletedAt(result.getDeletedAt().toString())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void reactChatMessage(ReactChatMessageRequest request, StreamObserver<ReactChatMessageResponse> responseObserver) {
		ReactChatMessageCommand command = new ReactChatMessageCommand(request.getRoomId(), request.getMessageId(), request
				.getReactorUserId(), request
						.getReactorPublicId(), request.getReactionType(), request.getReactionCode(), request.getAddRequested());

		ReactChatMessageEventResponseDTO result = chEngineChatCommandUseCase.reactChatMessage(command);

		ReactChatMessageResponse response = ReactChatMessageResponse.newBuilder()
				.setRoomId(result.getRoomId())
				.setMessageId(result.getMessageId())
				.setReactorPublicId(result.getReactorPublicId())
				.setReactionType(result.getReactionType())
				.setReactionCode(result.getReactionCode())
				.setAdded(result.getAdded())
				.setReactedAt(result.getReactedAt().toString())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void applyRoomNotice(ApplyRoomNoticeRequest request, StreamObserver<ApplyRoomNoticeResponse> responseObserver) {
		ApplyRoomNoticeCommand command = new ApplyRoomNoticeCommand(request.getRoomId(), request
				.getRoomNoticeAction(), request.hasTargetRoomNoticeId() ? request.getTargetRoomNoticeId() : null, request
						.getRoomNoticeType(), request.hasSourceMessageId() ? request.getSourceMessageId()
								: null, request.getRoomNoticeContents(), request.getRequesterUserId(), request.getRequesterPublicId());

		RoomNoticeViewResponseDTO result = chEngineChatCommandUseCase.applyRoomNotice(command);

		ApplyRoomNoticeResponse response = ApplyRoomNoticeResponse.newBuilder()
				.setRoomNoticeId(result.getRoomNoticeId())
				.setRoomId(result.getRoomId())
				.setRoomNoticeAction(result.getRoomNoticeAction())
				.setRoomNoticeType(result.getRoomNoticeType())
				.setRoomNoticeContents(result.getRoomNoticeContents())
				.setRoomNoticeStatus(result.getRoomNoticeStatus())
				.setApplierPublicId(result.getApplierPublicId())
				.setLastAppliedAt(result.getLastAppliedAt().toString())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
