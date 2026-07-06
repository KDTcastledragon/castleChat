package com.chat.wsgate.grpc;

import org.springframework.stereotype.Component;

import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;
import com.chat.contract.grpc.ApplyRoomNoticeRequest;
import com.chat.contract.grpc.ChEngineRoomGrpc;
import com.chat.wsgate.client.WsGateChEngineRoomClient;
import com.chat.wsgate.support.GrpcToDtoConverter;

import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
@Log4j2
public class WsGateChEngineRoomClientGrpc implements WsGateChEngineRoomClient {

	@GrpcClient("channel-engine")
	private ChEngineRoomGrpc.ChEngineRoomBlockingStub chEngineStub; // channel-engine gRPC 서버를 호출하기 위한 client 객체.
	//	ChengineGrpc는 네가 직접 만드는 일반 클래스가 아니야. chengine.proto를 작성하면 Gradle protobuf plugin이 자동 생성함.
	//	BlockingStub : 요청 보내고 응답 올 때까지 현재 thread가 기다리는 "동기" client.

	//	private GrpcToDtoConverter grpcToDtoConverter; // static utility라면 굳이 선언할 필요없음.

	// 계층 간 변환 : gRPC Response -> App DTO -> WebSocket JSON.
	//		gRPC returns는 proto message만 가능. Java DTO를 직접 response로 못 씀. 변환은 필수. 귀찮으면 helper로 분리.

	@Override
	public RoomNoticeViewResponseDTO applyRoomNotice(ApplyRoomNoticeCommand cmd) {
		//		ApplyRoomNoticeRequest cmdRequest = ApplyRoomNoticeRequest.newBuilder()
		//				.setRoomId(cmd.getRoomId())
		//				.setRoomNoticeAction(cmd.getRoomNoticeAction())
		//				.setTargetRoomNoticeId(cmd.getTargetRoomNoticeId())
		//				.setRoomNoticeType(cmd.getRoomNoticeType())
		//				.setSourceMessageId(cmd.getSourceMessageId())
		//				.setRoomNoticeContents(cmd.getRoomNoticeContents())
		//				.setRequesterUserId(cmd.getRequesterUserId())
		//				.setRequesterPublicId(cmd.getRequesterPublicId())
		//				.build();

		ApplyRoomNoticeRequest.Builder cmdRequestBuilder = ApplyRoomNoticeRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setRoomNoticeAction(cmd.getRoomNoticeAction())
				.setRoomNoticeType(cmd.getRoomNoticeType())
				.setRoomNoticeContents(cmd.getRoomNoticeContents())
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId());

		if (cmd.getTargetRoomNoticeId() != null) {
			cmdRequestBuilder.setTargetRoomNoticeId(cmd.getTargetRoomNoticeId());
		}

		if (cmd.getSourceMessageId() != null) {
			cmdRequestBuilder.setSourceMessageId(cmd.getSourceMessageId());
		}

		ApplyRoomNoticeRequest cmdRequest = cmdRequestBuilder.build();

		RoomNoticeViewResponseDTO grpcRoomNoticeResponse = GrpcToDtoConverter
				.convertGrpcToRoomNoticeViewResDto(chEngineStub.applyRoomNotice(cmdRequest));

		log.info("wsgate-gRPC RoomNotice = rom:{} rnId:{} action:{} status:{} / usr:{}", grpcRoomNoticeResponse.getRoomId(), grpcRoomNoticeResponse
				.getRoomNoticeId(), grpcRoomNoticeResponse
						.getRoomNoticeAction(), grpcRoomNoticeResponse.getRoomNoticeStatus(), cmd.getRequesterUserId());

		return grpcRoomNoticeResponse;
	}
}
