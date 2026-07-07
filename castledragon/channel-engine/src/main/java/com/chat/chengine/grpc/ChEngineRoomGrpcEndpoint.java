package com.chat.chengine.grpc;

import com.chat.chengine.usecase.RoomCommandUseCase;
import com.chat.contract.grpc.ApplyRoomNoticeRequest;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.ChEngineRoomGrpc;
import com.chat.contract.grpc.EnterRoomResponse;
import com.chat.contract.grpc.OpenDirectChatRoomRequest;
import com.chat.contract.room.command.ApplyRoomNoticeCommand;
import com.chat.contract.room.command.OpenDirectChatRoomCommand;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewResponseDTO;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Log4j2
public class ChEngineRoomGrpcEndpoint extends ChEngineRoomGrpc.ChEngineRoomImplBase {

	private final RoomCommandUseCase roomCommandUseCase;

	@Override
	public void openDirectChatRoom(OpenDirectChatRoomRequest request, StreamObserver<EnterRoomResponse> responseObserver) {
		OpenDirectChatRoomCommand openDirChtCmd = new OpenDirectChatRoomCommand(request.getRequesterUserId(), request
				.getRequesterPublicId(), request.getFriendPublicId());

		EnterRoomResponseDTO response = roomCommandUseCase.openDirectChatRoom(openDirChtCmd);

		EnterRoomResponse grpcResponse = DtoToGrpcConverter.convertEnterRoomResDtoToGrpc(response);

		responseObserver.onNext(grpcResponse);
		responseObserver.onCompleted();
	}

	@Override
	public void applyRoomNotice(ApplyRoomNoticeRequest request, StreamObserver<ApplyRoomNoticeResponse> responseObserver) {
		ApplyRoomNoticeCommand command = new ApplyRoomNoticeCommand(request.getRoomId(), request
				.getRoomNoticeAction(), request.hasTargetRoomNoticeId() ? request.getTargetRoomNoticeId() : null, request
						.getRoomNoticeType(), request.hasSourceMessageId() ? request.getSourceMessageId()
								: null, request.getRoomNoticeContents(), request.getRequesterUserId(), request.getRequesterPublicId());

		RoomNoticeViewResponseDTO result = roomCommandUseCase.applyRoomNotice(command);

		ApplyRoomNoticeResponse response = ApplyRoomNoticeResponse.newBuilder()
				.setRoomNoticeId(result.getRoomNoticeId())
				.setRoomId(result.getRoomId())
				.setRoomNoticeAction(result.getRoomNoticeAction())
				.setRoomNoticeType(result.getRoomNoticeType())
				.setRoomNoticeContents(result.getRoomNoticeContents())
				.setRoomNoticeStatus(result.getRoomNoticeStatus())
				.setRequesterPublicId(result.getRequesterPublicId())
				.setLastAppliedAt(result.getLastAppliedAt().toString())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
