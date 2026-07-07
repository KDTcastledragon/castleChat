package com.chat.wsgate.grpc;

import org.springframework.stereotype.Component;

import com.chat.contract.friend.command.AddFriendCommand;
import com.chat.contract.friend.command.RespondFriendCommand;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;
import com.chat.contract.grpc.AddFriendRequest;
import com.chat.contract.grpc.ChEngineFriendGrpc;
import com.chat.contract.grpc.RespondFriendRequest;
import com.chat.wsgate.client.WsGateFriendClient;
import com.chat.wsgate.support.GrpcToDtoConverter;

import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
@Log4j2
public class WsGateFriendClientGrpc implements WsGateFriendClient {

	@GrpcClient("channel-engine")
	private ChEngineFriendGrpc.ChEngineFriendBlockingStub chEngineFriendStub;

	@Override
	public FriendEventResponseDTO addFriend(AddFriendCommand cmd) {
		AddFriendRequest cmdRequest = AddFriendRequest.newBuilder()
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.setTargetPublicId(cmd.getTargetPublicId())
				.build();

		FriendEventResponseDTO response = GrpcToDtoConverter.convertGrpcToFriendEventResDto(chEngineFriendStub.addFriend(cmdRequest));

		log.info("wsgate-gRPC AddFriend = event:{} requester:{} target:{} status:{}", response.getFriendEventType(), response
				.getRequesterUserId(), response.getTargetUserId(), response.getFriendStatus());

		return response;
	}

	@Override
	public FriendEventResponseDTO respondFriend(RespondFriendCommand cmd) {
		RespondFriendRequest cmdRequest = RespondFriendRequest.newBuilder()
				.setResponderUserId(cmd.getResponderUserId())
				.setResponderPublicId(cmd.getResponderPublicId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.setFriendAction(cmd.getFriendAction())
				.build();

		FriendEventResponseDTO response = GrpcToDtoConverter.convertGrpcToFriendEventResDto(chEngineFriendStub.respondFriend(cmdRequest));

		log.info("wsgate-gRPC RespondFriend = event:{} requester:{} target:{} status:{}", response.getFriendEventType(), response
				.getRequesterUserId(), response.getTargetUserId(), response.getFriendStatus());

		return response;
	}
}