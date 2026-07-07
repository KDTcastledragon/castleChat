package com.chat.chengine.grpc;

import com.chat.chengine.support.DtoToGrpcConverter;
import com.chat.chengine.usecase.FriendCommandUseCase;
import com.chat.contract.friend.command.AddFriendCommand;
import com.chat.contract.friend.command.FindOnlineFriendTargetsCommand;
import com.chat.contract.friend.command.RespondFriendCommand;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;
import com.chat.contract.friend.domain.res.OnlineFriendTargetsResponseDTO;
import com.chat.contract.grpc.AddFriendRequest;
import com.chat.contract.grpc.ChEngineFriendGrpc;
import com.chat.contract.grpc.FindOnlineFriendTargetsRequest;
import com.chat.contract.grpc.FriendEventResponse;
import com.chat.contract.grpc.OnlineFriendTargetsResponse;
import com.chat.contract.grpc.RespondFriendRequest;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Log4j2
public class FriendGrpcEndpoint extends ChEngineFriendGrpc.ChEngineFriendImplBase {

	private final FriendCommandUseCase friendCommandUseCase;

	@Override
	public void addFriend(AddFriendRequest request, StreamObserver<FriendEventResponse> responseObserver) {
		AddFriendCommand command = new AddFriendCommand(request.getRequesterUserId(), request.getRequesterPublicId(), request.getTargetPublicId());

		FriendEventResponseDTO result = friendCommandUseCase.addFriend(command);

		FriendEventResponse response = DtoToGrpcConverter.convertFriendEventResDtoToGrpc(result);

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void respondFriend(RespondFriendRequest request, StreamObserver<FriendEventResponse> responseObserver) {
		RespondFriendCommand command = new RespondFriendCommand(request.getResponderUserId(), request.getResponderPublicId(), request
				.getRequesterPublicId(), request.getFriendAction());

		FriendEventResponseDTO result = friendCommandUseCase.respondFriend(command);

		FriendEventResponse response = DtoToGrpcConverter.convertFriendEventResDtoToGrpc(result);

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void findOnlineFriendTargets(FindOnlineFriendTargetsRequest request, StreamObserver<OnlineFriendTargetsResponse> responseObserver) {
		FindOnlineFriendTargetsCommand command = new FindOnlineFriendTargetsCommand(request.getUserId(), request.getPublicId());

		OnlineFriendTargetsResponseDTO result = friendCommandUseCase.findOnlineFriendTargets(command);

		OnlineFriendTargetsResponse response = DtoToGrpcConverter.convertOnlineFriendTargetsResDtoToGrpc(result);

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
