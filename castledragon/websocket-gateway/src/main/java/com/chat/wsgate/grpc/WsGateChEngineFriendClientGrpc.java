package com.chat.wsgate.grpc;

import org.springframework.stereotype.Component;

import com.chat.contract.friend.command.FindOnlineFriendTargetsCommand;
import com.chat.contract.friend.domain.res.OnlineFriendTargetsResponseDTO;
import com.chat.contract.grpc.ChEngineFriendGrpc;
import com.chat.contract.grpc.FindOnlineFriendTargetsRequest;
import com.chat.wsgate.client.WsGateChEngineFriendClient;
import com.chat.wsgate.support.GrpcToDtoConverter;

import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
@Log4j2
public class WsGateChEngineFriendClientGrpc implements WsGateChEngineFriendClient {

	@GrpcClient("channel-engine")
	private ChEngineFriendGrpc.ChEngineFriendBlockingStub chEngineFriendStub;

	@Override
	public OnlineFriendTargetsResponseDTO findOnlineFriendTargets(FindOnlineFriendTargetsCommand cmd) {
		FindOnlineFriendTargetsRequest cmdRequest = FindOnlineFriendTargetsRequest.newBuilder()
				.setUserId(cmd.getUserId())
				.setPublicId(cmd.getPublicId())
				.build();

		OnlineFriendTargetsResponseDTO response = GrpcToDtoConverter
				.convertGrpcToOnlineFriendTargetsResDto(chEngineFriendStub.findOnlineFriendTargets(cmdRequest));

		log.info("wsgate-gRPC OnlineFriendTargets = user:{} targets:{}", response.getUserId(), response.getTargetUserIds());

		return response;
	}
}
