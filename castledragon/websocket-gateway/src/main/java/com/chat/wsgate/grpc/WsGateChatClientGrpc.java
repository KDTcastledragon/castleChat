package com.chat.wsgate.grpc;

import org.springframework.stereotype.Component;

import com.chat.contract.chatting.command.CreateChatMessageCommand;
import com.chat.contract.chatting.command.DeleteChatMessageCommand;
import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.command.ReadChatMessageCommand;
import com.chat.contract.chatting.command.StartDirectChatCommand;
import com.chat.contract.chatting.command.StartGroupChatCommand;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.chatting.domain.res.DeleteChatMessageResponseDTO;
import com.chat.contract.chatting.domain.res.ReactChatMessageEventResponseDTO;
import com.chat.contract.chatting.domain.res.ReadPositionUpdateResponseDTO;
import com.chat.contract.chatting.domain.res.StartChatResponseDTO;
import com.chat.contract.grpc.ChEngineChatGrpc;
import com.chat.contract.grpc.CreateChatMessageRequest;
import com.chat.contract.grpc.DeleteChatMessageRequest;
import com.chat.contract.grpc.ReactChatMessageRequest;
import com.chat.contract.grpc.ReadChatMessageRequest;
import com.chat.contract.grpc.StartDirectChatRequest;
import com.chat.contract.grpc.StartGroupChatRequest;
import com.chat.wsgate.client.WsGateChatClient;
import com.chat.wsgate.support.GrpcToDtoConverter;

import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
@Log4j2
public class WsGateChatClientGrpc implements WsGateChatClient {

	@GrpcClient("channel-engine")
	private ChEngineChatGrpc.ChEngineChatBlockingStub chEngineStub; // channel-engine gRPC 서버를 호출하기 위한 client 객체.
	//	ChEngineGrpc는 네가 직접 만드는 일반 클래스가 아니야. chengine.proto를 작성하면 Gradle protobuf plugin이 자동 생성함.
	//	BlockingStub : 요청 보내고 응답 올 때까지 현재 thread가 기다리는 "동기" client.

	//	private GrpcToDtoConverter grpcToDtoConverter; // static utility라면 굳이 선언할 필요없음.

	// 계층 간 변환 : gRPC Response -> App DTO -> WebSocket JSON.
	//		gRPC returns는 proto message만 가능. Java DTO를 직접 response로 못 씀. 변환은 필수. 귀찮으면 helper로 분리.

	// ====== 1:1 채팅방 첫 메시지 전송 + 방 생성 요청 =======================================================================
	@Override
	public StartChatResponseDTO startDirectChat(StartDirectChatCommand cmd) {
		StartDirectChatRequest.Builder cmdRequestBuilder = StartDirectChatRequest.newBuilder()
				.setTargetPublicId(cmd.getTargetPublicId())
				.setSenderUserId(cmd.getSenderUserId())
				.setSenderPublicId(cmd.getSenderPublicId())
				.setMessageType(cmd.getMessageType())
				.setMessageText(cmd.getMessageText() == null ? "" : cmd.getMessageText());

		if (cmd.getReplyToMessageId() != null) {
			cmdRequestBuilder.setReplyToMessageId(cmd.getReplyToMessageId());
		}

		if (cmd.getAttachmentIds() != null && !cmd.getAttachmentIds().isEmpty()) {
			cmdRequestBuilder.addAllAttachmentIds(cmd.getAttachmentIds());
		}

		StartChatResponseDTO response = GrpcToDtoConverter
				.convertGrpcToStartChatResDto(chEngineStub.startDirectChat(cmdRequestBuilder.build()));

		log.info("wsgate-gRPC StartDirectChat = rom:{} mId:{} msg:{} / usr:{}", response.getEnterRoomInfo().getRoomId(), response
				.getFirstChatMessage().getMessageId(), response.getFirstChatMessage().getMessageText(), cmd.getSenderUserId());

		return response;
	}

	// ====== 단톡방 첫 메시지 전송 + 방 생성 요청 =======================================================================
	@Override
	public StartChatResponseDTO startGroupChat(StartGroupChatCommand cmd) {
		StartGroupChatRequest.Builder cmdRequestBuilder = StartGroupChatRequest.newBuilder()
				.setRoomName(cmd.getRoomName())
				.setRoomThumbnail(cmd.getRoomThumbnail() == null ? "" : cmd.getRoomThumbnail())
				.addAllInviteMemberPublicIds(cmd.getInviteMemberPublicIds())
				.setSenderUserId(cmd.getSenderUserId())
				.setSenderPublicId(cmd.getSenderPublicId())
				.setMessageType(cmd.getMessageType())
				.setMessageText(cmd.getMessageText() == null ? "" : cmd.getMessageText());

		if (cmd.getReplyToMessageId() != null) {
			cmdRequestBuilder.setReplyToMessageId(cmd.getReplyToMessageId());
		}

		if (cmd.getAttachmentIds() != null && !cmd.getAttachmentIds().isEmpty()) {
			cmdRequestBuilder.addAllAttachmentIds(cmd.getAttachmentIds());
		}

		StartChatResponseDTO response = GrpcToDtoConverter
				.convertGrpcToStartChatResDto(chEngineStub.startGroupChat(cmdRequestBuilder.build()));

		log.info("wsgate-gRPC StartGroupChat = rom:{} mId:{} msg:{} / usr:{}", response.getEnterRoomInfo().getRoomId(), response
				.getFirstChatMessage().getMessageId(), response.getFirstChatMessage().getMessageText(), cmd.getSenderUserId());

		return response;
	}

	// ====== 메시지 전송 요청 =================================================================================================================
	@Override
	public ChatMessageViewResponseDTO createChatMessage(CreateChatMessageCommand cmd) {
		CreateChatMessageRequest.Builder cmdRequestBuilder = CreateChatMessageRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setSenderUserId(cmd.getSenderUserId())
				.setSenderPublicId(cmd.getSenderPublicId())
				.setMessageType(cmd.getMessageType())
				.setMessageText(cmd.getMessageText() == null ? "" : cmd.getMessageText());

		if (cmd.getReplyToMessageId() != null) {
			cmdRequestBuilder.setReplyToMessageId(cmd.getReplyToMessageId());
		}

		if (cmd.getAttachmentIds() != null && !cmd.getAttachmentIds().isEmpty()) {
			cmdRequestBuilder.addAllAttachmentIds(cmd.getAttachmentIds());
		}

		CreateChatMessageRequest cmdRequest = cmdRequestBuilder.build();

		ChatMessageViewResponseDTO gRpcSendMsgResponse = GrpcToDtoConverter
				.convertGrpcToChatMsgViewDto(chEngineStub.createChatMessage(cmdRequest));

		log.info("wsgate-gRPC send = mId:{} rom:{} msg:{} / usr:{}", gRpcSendMsgResponse.getMessageId(), gRpcSendMsgResponse
				.getRoomId(), gRpcSendMsgResponse.getMessageText(), cmd.getSenderUserId());

		return gRpcSendMsgResponse;
	}

	// ====== 메시지 읽음 처리 요청 =============================================================================================================
	@Override
	public ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand cmd) {
		ReadChatMessageRequest cmdRequest = ReadChatMessageRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setReaderUserId(cmd.getReaderUserId())
				.setReaderPublicId(cmd.getReaderPublicId())
				.setLastReadMessageId(cmd.getLastReadMessageId())
				.build();

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
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.build();

		DeleteChatMessageResponseDTO gRpcDeleteMsgResponse = GrpcToDtoConverter
				.convertGrpcToDeleteChatMsgResDto(chEngineStub.deleteChatMessage(cmdRequest));

		log.info("wsgate-gRPC Delete = rom:{} mId:{} stut:{} / usr:{}", gRpcDeleteMsgResponse.getRoomId(), gRpcDeleteMsgResponse
				.getMessageId(), gRpcDeleteMsgResponse.getMessageStatus(), cmd.getRequesterUserId());

		return gRpcDeleteMsgResponse;
	}

	@Override
	public ReactChatMessageEventResponseDTO reactChatMessage(ReactChatMessageCommand cmd) {
		ReactChatMessageRequest cmdRequest = ReactChatMessageRequest.newBuilder()
				.setRoomId(cmd.getRoomId())
				.setMessageId(cmd.getMessageId())
				.setRequesterUserId(cmd.getRequesterUserId())
				.setRequesterPublicId(cmd.getRequesterPublicId())
				.setReactionType(cmd.getReactionType())
				.setReactionCode(cmd.getReactionCode())
				.setAddRequested(cmd.getAddRequested())
				.build();

		ReactChatMessageEventResponseDTO gRpcRctMsgEvtResponse = GrpcToDtoConverter
				.convertGrpcToReactChatMsgEventResDto(chEngineStub.reactChatMessage(cmdRequest));

		log.info("wsgate-gRPC React = rom:{} mId:{} type:{} code:{} added:{} / usr:{}", gRpcRctMsgEvtResponse.getRoomId(), gRpcRctMsgEvtResponse
				.getMessageId(), gRpcRctMsgEvtResponse
						.getReactionType(), gRpcRctMsgEvtResponse.getReactionCode(), gRpcRctMsgEvtResponse.getAdded(), cmd.getRequesterUserId());

		return gRpcRctMsgEvtResponse;
	}
}
