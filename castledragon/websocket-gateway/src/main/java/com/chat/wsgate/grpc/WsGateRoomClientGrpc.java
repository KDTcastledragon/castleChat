package com.chat.wsgate.grpc;

import org.springframework.stereotype.Component;

import com.chat.contract.grpc.ApplyRoomNoticeRequest;
import com.chat.contract.grpc.BanMemberRequest;
import com.chat.contract.grpc.ChEngineRoomGrpc;
import com.chat.contract.grpc.ChangeMemberRoleRequest;
import com.chat.contract.grpc.EnterRoomRequest;
import com.chat.contract.grpc.InviteMemberRequest;
import com.chat.contract.grpc.KickMemberRequest;
import com.chat.contract.grpc.LeftRoomRequest;
import com.chat.contract.grpc.OpenDirectChatRoomRequest;
import com.chat.contract.room.command.ApplyRoomNoticeCommand;
import com.chat.contract.room.command.BanMemberCommand;
import com.chat.contract.room.command.ChangeMemberRoleCommand;
import com.chat.contract.room.command.EnterRoomCommand;
import com.chat.contract.room.command.InviteMemberCommand;
import com.chat.contract.room.command.KickMemberCommand;
import com.chat.contract.room.command.LeftRoomCommand;
import com.chat.contract.room.command.OpenDirectChatRoomCommand;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomFeedResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeApplyResponseDTO;
import com.chat.wsgate.client.WsGateRoomClient;
import com.chat.wsgate.support.GrpcToDtoConverter;

import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
@Log4j2
public class WsGateRoomClientGrpc implements WsGateRoomClient {

	@GrpcClient("channel-engine")
	private ChEngineRoomGrpc.ChEngineRoomBlockingStub chEngineRoomStub; // channel-engine gRPC 서버를 호출하기 위한 client 객체.
	//	ChEngineGrpc는 네가 직접 만드는 일반 클래스가 아니야. chengine.proto를 작성하면 Gradle protobuf plugin이 자동 생성함.
	//	BlockingStub : 요청 보내고 응답 올 때까지 현재 thread가 기다리는 "동기" client.

	//	private GrpcToDtoConverter grpcToDtoConverter; // static utility라면 굳이 선언할 필요없음.

	// 계층 간 변환 : gRPC Response -> App DTO -> WebSocket JSON.
	//		gRPC returns는 proto message만 가능. Java DTO를 직접 response로 못 씀. 변환은 필수. 귀찮으면 helper로 분리.

	@Override
	public EnterRoomResponseDTO openDirectChatRoom(OpenDirectChatRoomCommand cmd) {
		OpenDirectChatRoomRequest cmdRequest = OpenDirectChatRoomRequest.newBuilder()
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.setFriendPublicId(cmd.getFriendPublicId())
				.build();

		EnterRoomResponseDTO gRpcOpenDirChtResponse = GrpcToDtoConverter
				.convertGrpcToEnterRoomResDto(chEngineRoomStub.openDirectChatRoom(cmdRequest));

		log.info("wsgate-gRPC OpenDirectChat = rom:{} / usr:{}", gRpcOpenDirChtResponse.getRoomId(), cmd.getRequesterUserId());

		return gRpcOpenDirChtResponse;
	}

	@Override
	public EnterRoomResponseDTO enterRoom(EnterRoomCommand cmd) {
		EnterRoomRequest cmdRequest = EnterRoomRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.build();

		EnterRoomResponseDTO response = GrpcToDtoConverter.convertGrpcToEnterRoomResDto(chEngineRoomStub.enterRoom(cmdRequest));

		log.info("wsgate-gRPC EnterRoom = rom:{} / usr:{}", response.getRoomId(), cmd.getRequesterUserId());

		return response;
	}

	@Override
	public RoomFeedResponseDTO leftRoom(LeftRoomCommand cmd) {
		LeftRoomRequest cmdRequest = LeftRoomRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.build();

		RoomFeedResponseDTO response = GrpcToDtoConverter.convertGrpcToRoomFeedResDto(chEngineRoomStub.leftRoom(cmdRequest));

		log.info("wsgate-gRPC LeftRoom = rom:{} feed:{} / usr:{}", response.getRoomId(), response.getFeedType(), cmd.getRequesterUserId());

		return response;
	}

	@Override
	public RoomFeedResponseDTO inviteMember(InviteMemberCommand cmd) {
		InviteMemberRequest cmdRequest = InviteMemberRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.addAllInviteMemberPublicIds(cmd.getInviteTargetMemberPublicIds())
				.build();

		RoomFeedResponseDTO response = GrpcToDtoConverter.convertGrpcToRoomFeedResDto(chEngineRoomStub.inviteMember(cmdRequest));

		log.info("wsgate-gRPC InviteMember = rom:{} feed:{} / usr:{}", response.getRoomId(), response.getFeedType(), cmd.getRequesterUserId());

		return response;
	}

	@Override
	public RoomFeedResponseDTO kickMember(KickMemberCommand cmd) {
		KickMemberRequest cmdRequest = KickMemberRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.setKickTargetPublicId(cmd.getKickTargetPublicId())
				.build();

		RoomFeedResponseDTO response = GrpcToDtoConverter.convertGrpcToRoomFeedResDto(chEngineRoomStub.kickMember(cmdRequest));

		log.info("wsgate-gRPC KickMember = rom:{} feed:{} / usr:{}", response.getRoomId(), response.getFeedType(), cmd.getRequesterUserId());

		return response;
	}

	@Override
	public RoomFeedResponseDTO banMember(BanMemberCommand cmd) {
		BanMemberRequest cmdRequest = BanMemberRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.setBanTargetPublicId(cmd.getBanTargetPublicId())
				.build();

		RoomFeedResponseDTO response = GrpcToDtoConverter.convertGrpcToRoomFeedResDto(chEngineRoomStub.banMember(cmdRequest));

		log.info("wsgate-gRPC BanMember = rom:{} feed:{} / usr:{}", response.getRoomId(), response.getFeedType(), cmd.getRequesterUserId());

		return response;
	}

	@Override
	public RoomFeedResponseDTO changeMemberRole(ChangeMemberRoleCommand cmd) {
		ChangeMemberRoleRequest cmdRequest = ChangeMemberRoleRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.setTargetPublicId(cmd.getTargetPublicId())
				.setTargetRole(cmd.getTargetRole())
				.build();

		RoomFeedResponseDTO response = GrpcToDtoConverter.convertGrpcToRoomFeedResDto(chEngineRoomStub.changeMemberRole(cmdRequest));

		log.info("wsgate-gRPC ChangeMemberRole = rom:{} feed:{} / usr:{}", response.getRoomId(), response.getFeedType(), cmd.getRequesterUserId());

		return response;
	}

	@Override
	public RoomNoticeApplyResponseDTO applyRoomNotice(ApplyRoomNoticeCommand cmd) {
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

		RoomNoticeApplyResponseDTO grpcRoomNoticeApplyResponse = GrpcToDtoConverter
				.convertGrpcToRoomNoticeApplyResDto(chEngineRoomStub.applyRoomNotice(cmdRequest));

		log.info("wsgate-gRPC RoomNotice = rId:{} rnId:{} act:{} stat:{} / usr:{}", grpcRoomNoticeApplyResponse.getRoomNoticeView()
				.getRoomId(), grpcRoomNoticeApplyResponse.getRoomNoticeView().getRoomNoticeId(), grpcRoomNoticeApplyResponse.getRoomNoticeView()
						.getRoomNoticeAction(), grpcRoomNoticeApplyResponse.getRoomNoticeView().getRoomNoticeStatus(), cmd.getRequesterUserId());

		return grpcRoomNoticeApplyResponse;
	}
}
