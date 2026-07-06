package com.chat.chengine.grpc;

import com.chat.chengine.usecase.ChEngineRoomCommandUseCase;
import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;
import com.chat.contract.grpc.ApplyRoomNoticeRequest;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.ChEngineRoomGrpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Log4j2
public class ChEngineRoomGrpcEndpoint extends ChEngineRoomGrpc.ChEngineRoomImplBase {

	private final ChEngineRoomCommandUseCase chEngineRoomCommandUseCase;

	@Override
	public void applyRoomNotice(ApplyRoomNoticeRequest request, StreamObserver<ApplyRoomNoticeResponse> responseObserver) {
		ApplyRoomNoticeCommand command = new ApplyRoomNoticeCommand(request.getRoomId(), request
				.getRoomNoticeAction(), request.hasTargetRoomNoticeId() ? request.getTargetRoomNoticeId() : null, request
						.getRoomNoticeType(), request.hasSourceMessageId() ? request.getSourceMessageId()
								: null, request.getRoomNoticeContents(), request.getRequesterUserId(), request.getRequesterPublicId());

		RoomNoticeViewResponseDTO result = chEngineRoomCommandUseCase.applyRoomNotice(command);

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
