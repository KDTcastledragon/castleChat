package com.chat.chatorc.grpc;

import com.chat.chatorc.usecase.OrcChatCommandUseCase;
import com.chat.contract.command.chatting.CreateChatMessageCommand;
import com.chat.contract.command.chatting.DeleteChatMessageCommand;
import com.chat.contract.command.chatting.ReactChatMessageCommand;
import com.chat.contract.command.chatting.ReadChatMessageCommand;
import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.DeleteChatMessageResponseDTO;
import com.chat.contract.domain.chatting.ReactChatMessageEventResponseDTO;
import com.chat.contract.domain.chatting.ReadPositionUpdateResponseDTO;
import com.chat.contract.domain.room.RoomNoticeResponseDTO;
import com.chat.contract.grpc.ChatOrcGrpc;
import com.chat.contract.grpc.CreateChatMessageRequest;
import com.chat.contract.grpc.CreateChatMessageResponse;
import com.chat.contract.grpc.ReadChatMessageRequest;
import com.chat.contract.grpc.ReadChatMessageResponse;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.server.service.GrpcService;

// TCP Request 받는 곳. 정확히는 gRPC 서버가 받는다.

@GrpcService
@RequiredArgsConstructor
@Log4j2
public class ChatOrcGrpcEndpoint extends ChatOrcGrpc.ChatOrcImplBase {
	// ChatOrcGrpc.ChatOrcImplBase : proto에서 생성된 서버 계약.

	private final OrcChatCommandUseCase orcChatCommandUseCase;
	// private final ChatOrcQueryUseCase chatOrcQueryUseCase; // 오류때문에 뺀다.
	// Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: 
	// Unsatisfied dependency expressed through constructor parameter 1: No qualifying bean of type 'com.chat.chatorc.usecase.ChatOrcQueryUseCase' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}

	@Override
	public void createChatMessage(CreateChatMessageRequest request, StreamObserver<CreateChatMessageResponse> responseObserver) {
		// 1. proto request -> 내부 command 변환
		// 2. usecase 호출
		// 3. 결과 -> proto response 변환

		// gRPC generated type을 service/usecase 안으로 끌고 들어오지 않기 위해서 cmd 생성.
		CreateChatMessageCommand requestedCommand = new CreateChatMessageCommand(request.getRoomId(), request.getSenderUserId(), request
				.getSenderPublicId(), request.getMessageText());

		ChatMessageViewResponseDTO cmdResult = orcChatCommandUseCase.createChatMessage(requestedCommand);
		// 여기 ,, chatMessageDTO 내부 전용으로 해야하나????????????????????????????????????????????????????????????????????????????????????
		// 여기 ,, chatMessageDTO 내부 전용으로 해야하나????????????????????????????????????????????????????????????????????????????????????
		// 여기 ,, chatMessageDTO 내부 전용으로 해야하나????????????????????????????????????????????????????????????????????????????????????

		CreateChatMessageResponse response = CreateChatMessageResponse.newBuilder()
				.setMessageId(cmdResult.getMessageId())
				.setRoomId(cmdResult.getRoomId())
				.setSenderPublicId(cmdResult.getSenderPublicId())
				.setMessageText(cmdResult.getMessageText())
				.setCreatedAt(cmdResult.getCreatedAt().toString())
				.setUnreadCount(cmdResult.getUnreadCount())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	//====== 	
	@Override
	public void readChatMessage(ReadChatMessageRequest request, StreamObserver<ReadChatMessageResponse> responseObserver) {
		ReadChatMessageCommand requestCommand = new ReadChatMessageCommand(request.getRoomId(), request.getReaderUserId(), request
				.getReaderPublicId(), request.getLastReadMessageId());

		log.info("gRPC EndPoint readCmd : {}", requestCommand);

		ReadPositionUpdateResponseDTO cmdResult = orcChatCommandUseCase.readChatMessage(requestCommand);

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

		DeleteChatMessageResponseDTO result = orcChatCommandUseCase.deleteChatMessage(command);

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
				.getReactorUserId(), request.getReactorPublicId(), request.getReactionType(), request.getReactionCode());

		ReactChatMessageEventResponseDTO result = orcChatCommandUseCase.reactChatMessage(command);

		ReactChatMessageResponse response = ReactChatMessageResponse.newBuilder()
				.setRoomId(result.getRoomId())
				.setMessageId(result.getMessageId())
				.setReactorPublicId(result.getReactorPublicId())
				.setReactionType(result.getReactionType())
				.setReactionCode(result.getReactionCode())
				.setReacted(result.getReacted())
				.setReactedAt(result.getReactedAt().toString())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void createRoomNotice(CreateRoomNoticeRequest request, StreamObserver<CreateRoomNoticeResponse> responseObserver) {
		ApplyRoomNoticeCommand command = new ApplyRoomNoticeCommand(request.getRoomId(), request.getRoomNoticeType(), request.getSourceMessageId() == 0
				? null
				: request.getSourceMessageId(), request.getRoomNoticeContents(), request.getCreatorUserId(), request.getCreatorPublicId());

		RoomNoticeResponseDTO result = orcChatCommandUseCase.createRoomNotice(command);

		CreateRoomNoticeResponse response = CreateRoomNoticeResponse.newBuilder()
				.setRoomNoticeId(result.getRoomNoticeId())
				.setRoomId(result.getRoomId())
				.setRoomNoticeType(result.getRoomNoticeType())
				.setSourceMessageId(result.getSourceMessageId() == null ? 0L : result.getSourceMessageId())
				.setRoomNoticeContents(result.getRoomNoticeContents())
				.setCreatorPublicId(result.getCreatorPublicId())
				.setRoomNoticeStatus(result.getRoomNoticeStatus())
				.setCreatedAt(result.getCreatedAt().toString())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}