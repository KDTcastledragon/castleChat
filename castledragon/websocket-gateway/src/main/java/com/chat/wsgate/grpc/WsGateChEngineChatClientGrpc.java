package com.chat.wsgate.grpc;

import org.springframework.stereotype.Component;

import com.chat.contract.command.chatting.CreateChatMessageCommand;
import com.chat.contract.command.chatting.DeleteChatMessageCommand;
import com.chat.contract.command.chatting.ReactChatMessageCommand;
import com.chat.contract.command.chatting.ReadChatMessageCommand;
import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.DeleteChatMessageResponseDTO;
import com.chat.contract.domain.chatting.ReactChatMessageEventResponseDTO;
import com.chat.contract.domain.chatting.ReadPositionUpdateResponseDTO;
import com.chat.contract.grpc.ChEngineChatGrpc;
import com.chat.contract.grpc.CreateChatMessageRequest;
import com.chat.contract.grpc.DeleteChatMessageRequest;
import com.chat.contract.grpc.ReactChatMessageRequest;
import com.chat.contract.grpc.ReadChatMessageRequest;
import com.chat.wsgate.client.WsGateChEngineChatClient;
import com.chat.wsgate.support.GrpcToDtoConverter;

import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
@Log4j2
public class WsGateChEngineChatClientGrpc implements WsGateChEngineChatClient {

	@GrpcClient("channel-engine")
	private ChEngineChatGrpc.ChEngineChatBlockingStub chEngineStub; // channel-engine gRPC 서버를 호출하기 위한 client 객체.
	//	ChengineGrpc는 네가 직접 만드는 일반 클래스가 아니야. chengine.proto를 작성하면 Gradle protobuf plugin이 자동 생성함.
	//	BlockingStub : 요청 보내고 응답 올 때까지 현재 thread가 기다리는 "동기" client.

	//	private GrpcToDtoConverter grpcToDtoConverter; // static utility라면 굳이 선언할 필요없음.

	// 계층 간 변환 : gRPC Response -> App DTO -> WebSocket JSON.
	//		gRPC returns는 proto message만 가능. Java DTO를 직접 response로 못 씀. 변환은 필수. 귀찮으면 helper로 분리.

	//====== chengine의 gRPC-end-Point로 전송한 메시지 DB Insert 요청 =============================================================================================================
	@Override
	public ChatMessageViewResponseDTO createChatMessage(CreateChatMessageCommand cmd) {
		CreateChatMessageRequest cmdRequest = CreateChatMessageRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setSenderUserId(cmd.getSenderUserId())
				.setSenderPublicId(cmd.getSenderPublicId())
				.setMessageText(cmd.getMessageText())
				.build();

		//		log.info("gRPC send Req: {}", commandRequest.getRoomId(),commandRequest.getSenderUserId(),commandRequest.getSenderPublicId(),commandRequest.getMessageText());

		ChatMessageViewResponseDTO gRpcSendMsgResponse = GrpcToDtoConverter
				.convertGrpcToChatMsgViewDto(chEngineStub.createChatMessage(cmdRequest));
		log.info("wsgate-gRPC send = mId:{} rom:{} msg:{} / usr:{}", gRpcSendMsgResponse.getMessageId(), gRpcSendMsgResponse
				.getRoomId(), gRpcSendMsgResponse.getMessageText(), cmd.getSenderUserId());
		// --> ws-gate의 gRPC client outBound call point. endPoint는 받는 쪽이다.

		return gRpcSendMsgResponse;
	}

	//====== chengine의 gRPC-end-Point로 메시지 읽음 처리 요청 =============================================================================================================
	@Override
	public ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand cmd) {
		ReadChatMessageRequest cmdRequest = ReadChatMessageRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setReaderUserId(cmd.getReaderUserId())
				.setReaderPublicId(cmd.getReaderPublicId())
				.setLastReadMessageId(cmd.getLastReadMessageId())
				.build();

		//		log.info("gRPC read Req: {}", commandRequest.getRoomId(),commandRequest.getReaderUserId(),commandRequest.getLastReadMessageId());

		ReadPositionUpdateResponseDTO gRpcReadMsgResponse = GrpcToDtoConverter
				.convertGrpcToReadPosUpdateResDto(chEngineStub.readChatMessage(cmdRequest));

		log.info("wsgate-gRPC Read = rom:{} old:{} new:{} / usr:{}", gRpcReadMsgResponse.getRoomId(), gRpcReadMsgResponse
				.getOldLastReadMessageId(), gRpcReadMsgResponse.getLastReadMessageId(), cmd.getReaderUserId());

		return gRpcReadMsgResponse;
	}

	@Override
	public DeleteChatMessageResponseDTO deleteChatMessage(DeleteChatMessageCommand cmd) {
		DeleteChatMessageRequest cmdRequest = DeleteChatMessageRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setMessageId(cmd.getMessageId())
				.setDeleterUserId(cmd.getDeleterUserId())
				.setDeleterPublicId(cmd.getDeleterPublicId())
				.build();

		DeleteChatMessageResponseDTO gRpcDeleteMsgResponse = GrpcToDtoConverter
				.convertGrpcToDeleteChatMsgResDto(chEngineStub.deleteChatMessage(cmdRequest));

		log.info("wsgate-gRPC Delete = rom:{} mId:{} stut:{} / usr:{}", gRpcDeleteMsgResponse.getRoomId(), gRpcDeleteMsgResponse
				.getMessageId(), gRpcDeleteMsgResponse.getMessageStatus(), cmd.getDeleterUserId());

		return gRpcDeleteMsgResponse;
	}

	@Override
	public ReactChatMessageEventResponseDTO reactChatMessage(ReactChatMessageCommand cmd) {
		ReactChatMessageRequest cmdRequest = ReactChatMessageRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setMessageId(cmd.getMessageId())
				.setReactorUserId(cmd.getReactorUserId())
				.setReactorPublicId(cmd.getReactorPublicId())
				.setReactionType(cmd.getReactionType())
				.setReactionCode(cmd.getReactionCode())
				.setAddRequested(cmd.getAddRequested())
				.build();

		ReactChatMessageEventResponseDTO gRpcRctMsgEvtResponse = GrpcToDtoConverter
				.convertGrpcToReactChatMsgEventResDto(chEngineStub.reactChatMessage(cmdRequest));

		log.info("wsgate-gRPC React = rom:{} mId:{} type:{} code:{} added:{} / usr:{}", gRpcRctMsgEvtResponse.getRoomId(), gRpcRctMsgEvtResponse
				.getMessageId(), gRpcRctMsgEvtResponse
						.getReactionType(), gRpcRctMsgEvtResponse.getReactionCode(), gRpcRctMsgEvtResponse.getAdded(), cmd.getReactorUserId());

		return gRpcRctMsgEvtResponse;
	}

}
