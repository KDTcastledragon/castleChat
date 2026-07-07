package com.chat.chengine.grpc;

import com.chat.chengine.support.DtoToGrpcConverter;
import com.chat.chengine.usecase.RoomCommandUseCase;
import com.chat.contract.grpc.ApplyRoomNoticeRequest;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.BanMemberRequest;
import com.chat.contract.grpc.ChEngineRoomGrpc;
import com.chat.contract.grpc.ChangeMemberRoleRequest;
import com.chat.contract.grpc.EnterRoomRequest;
import com.chat.contract.grpc.EnterRoomResponse;
import com.chat.contract.grpc.InviteMemberRequest;
import com.chat.contract.grpc.KickMemberRequest;
import com.chat.contract.grpc.LeftRoomRequest;
import com.chat.contract.grpc.OpenDirectChatRoomRequest;
import com.chat.contract.grpc.RoomFeedResponse;
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

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Log4j2
public class RoomGrpcEndpoint extends ChEngineRoomGrpc.ChEngineRoomImplBase {

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
	public void enterRoom(EnterRoomRequest request, StreamObserver<EnterRoomResponse> responseObserver) {
		EnterRoomCommand enterRomCmd = new EnterRoomCommand(request.getRoomId(), request.getRequesterUserId(), request.getRequesterPublicId());

		EnterRoomResponseDTO response = roomCommandUseCase.enterRoom(enterRomCmd);

		EnterRoomResponse grpcResponse = DtoToGrpcConverter.convertEnterRoomResDtoToGrpc(response);

		responseObserver.onNext(grpcResponse);
		responseObserver.onCompleted();
	}

	@Override
	public void leftRoom(LeftRoomRequest request, StreamObserver<RoomFeedResponse> responseObserver) {
		LeftRoomCommand leftRomCmd = new LeftRoomCommand(request.getRoomId(), request.getRequesterUserId(), request.getRequesterPublicId());

		RoomFeedResponseDTO response = roomCommandUseCase.leftRoom(leftRomCmd);

		RoomFeedResponse grpcResponse = DtoToGrpcConverter.convertRoomFeedResDtoToGrpc(response);

		responseObserver.onNext(grpcResponse);
		responseObserver.onCompleted();
	}

	@Override
	public void inviteMember(InviteMemberRequest request, StreamObserver<RoomFeedResponse> responseObserver) {
		InviteMemberCommand ivtMbrCmd = new InviteMemberCommand(request.getRoomId(), request.getRequesterUserId(), request
				.getRequesterPublicId(), request.getInviteMemberPublicIdsList());

		RoomFeedResponseDTO response = roomCommandUseCase.inviteMember(ivtMbrCmd);

		RoomFeedResponse grpcResponse = DtoToGrpcConverter.convertRoomFeedResDtoToGrpc(response);

		responseObserver.onNext(grpcResponse);
		responseObserver.onCompleted();
	}

	@Override
	public void kickMember(KickMemberRequest request, StreamObserver<RoomFeedResponse> responseObserver) {
		KickMemberCommand kickMbrCmd = new KickMemberCommand(request.getRoomId(), request.getRequesterUserId(), request
				.getRequesterPublicId(), request.getKickTargetPublicId());

		RoomFeedResponseDTO response = roomCommandUseCase.kickMember(kickMbrCmd);

		RoomFeedResponse grpcResponse = DtoToGrpcConverter.convertRoomFeedResDtoToGrpc(response);

		responseObserver.onNext(grpcResponse);
		responseObserver.onCompleted();
	}

	@Override
	public void banMember(BanMemberRequest request, StreamObserver<RoomFeedResponse> responseObserver) {
		BanMemberCommand banMbrCmd = new BanMemberCommand(request.getRoomId(), request.getRequesterUserId(), request
				.getRequesterPublicId(), request.getBanTargetPublicId());

		RoomFeedResponseDTO response = roomCommandUseCase.banMember(banMbrCmd);

		RoomFeedResponse grpcResponse = DtoToGrpcConverter.convertRoomFeedResDtoToGrpc(response);

		responseObserver.onNext(grpcResponse);
		responseObserver.onCompleted();
	}

	@Override
	public void changeMemberRole(ChangeMemberRoleRequest request, StreamObserver<RoomFeedResponse> responseObserver) {
		ChangeMemberRoleCommand chgMbrRolCmd = new ChangeMemberRoleCommand(request.getRoomId(), request.getRequesterUserId(), request
				.getRequesterPublicId(), request.getTargetPublicId(), request.getTargetRole());

		RoomFeedResponseDTO response = roomCommandUseCase.changeMemberRole(chgMbrRolCmd);

		RoomFeedResponse grpcResponse = DtoToGrpcConverter.convertRoomFeedResDtoToGrpc(response);

		responseObserver.onNext(grpcResponse);
		responseObserver.onCompleted();
	}

	@Override
	public void applyRoomNotice(ApplyRoomNoticeRequest request, StreamObserver<ApplyRoomNoticeResponse> responseObserver) {
		ApplyRoomNoticeCommand command = new ApplyRoomNoticeCommand(request.getRoomId(), request
				.getRoomNoticeAction(), request.hasTargetRoomNoticeId() ? request.getTargetRoomNoticeId() : null, request
						.getRoomNoticeType(), request.hasSourceMessageId() ? request.getSourceMessageId()
								: null, request.getRoomNoticeContents(), request.getRequesterUserId(), request.getRequesterPublicId());

		RoomNoticeApplyResponseDTO result = roomCommandUseCase.applyRoomNotice(command);

		ApplyRoomNoticeResponse response = DtoToGrpcConverter.convertRoomNoticeApplyResDtoToGrpc(result);

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
