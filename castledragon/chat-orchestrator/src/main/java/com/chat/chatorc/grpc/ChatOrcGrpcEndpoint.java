package com.chat.chatorc.grpc;

import com.chat.chatorc.usecase.ChatOrcCommandUseCase;
import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.command.ReadChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ReadPositionUpdateResponseDTO;
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

	private final ChatOrcCommandUseCase chatOrcCommandUseCase;
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

		ChatMessageViewDTO cmdResult = chatOrcCommandUseCase.createChatMessage(requestedCommand);
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

		ReadPositionUpdateResponseDTO cmdResult = chatOrcCommandUseCase.readChatMessage(requestCommand);

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
}