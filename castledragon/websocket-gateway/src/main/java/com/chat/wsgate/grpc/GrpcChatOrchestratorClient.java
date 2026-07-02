package com.chat.wsgate.grpc;

import org.springframework.stereotype.Component;

import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.command.ReadChatMessageCommand;
import com.chat.contract.convert.GrpcToDtoConverter;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ReadPositionUpdateResponseDTO;
import com.chat.contract.grpc.ChatOrcGrpc;
import com.chat.contract.grpc.CreateChatMessageRequest;
import com.chat.contract.grpc.ReadChatMessageRequest;
import com.chat.wsgate.client.ChatOrchestratorClient;

import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
@Log4j2
public class GrpcChatOrchestratorClient implements ChatOrchestratorClient {

	@GrpcClient("chat-orchestrator")
	private ChatOrcGrpc.ChatOrcBlockingStub chatOrcStub; // chat-orchestrator gRPC 서버를 호출하기 위한 client 객체.
	//	ChatOrcGrpc는 네가 직접 만드는 일반 클래스가 아니야. chat_orc.proto를 작성하면 Gradle protobuf plugin이 자동 생성함.
	//	BlockingStub : 요청 보내고 응답 올 때까지 현재 thread가 기다리는 "동기" client.

	//	private GrpcToDtoConverter grpcToDtoConverter; // static utility라면 굳이 선언할 필요없음.

	//====== chatOrc의 gRPC-end-Point로 전송한 메시지 DB Insert 요청 =============================================================================================================
	@Override
	public ChatMessageViewDTO createChatMessage(CreateChatMessageCommand command) {
		CreateChatMessageRequest commandRequest = CreateChatMessageRequest.newBuilder()
				.setRoomId(command.getRoomId())
				.setSenderUserId(command.getSenderUserId())
				.setSenderPublicId(command.getSenderPublicId())
				.setMessageText(command.getMessageText())
				.build();

		//		log.info("gRPC send Req: {}", commandRequest.getRoomId(),commandRequest.getSenderUserId(),commandRequest.getSenderPublicId(),commandRequest.getMessageText());

		// 계층 간 변환 : gRPC Response -> App DTO -> WebSocket JSON.
		//		gRPC returns는 proto message만 가능. Java DTO를 직접 response로 못 씀. 변환은 필수. 귀찮으면 helper로 분리.

		ChatMessageViewDTO createdMsgResponse = GrpcToDtoConverter.convertGrpcToChatMsgViewDto(chatOrcStub.createChatMessage(commandRequest));
		log.info("gate-gRPC send 응답 : mId:{} rom:{} msg:{}", createdMsgResponse.getMessageId(), createdMsgResponse.getRoomId(), createdMsgResponse
				.getMessageText());
		// --> ws-gate의 gRPC client outBound call point. endPoint는 받는 쪽이다.

		return createdMsgResponse;
	}

	//====== chatOrc의 gRPC-end-Point로 메시지 읽음 처리 요청 =============================================================================================================
	@Override
	public ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand command) {
		ReadChatMessageRequest commandRequest = ReadChatMessageRequest.newBuilder()
				.setRoomId(command.getRoomId())
				.setReaderUserId(command.getReaderUserId())
				.setReaderPublicId(command.getReaderPublicId())
				.setLastReadMessageId(command.getLastReadMessageId())
				.build();

		//		log.info("gRPC read Req: {}", commandRequest.getRoomId(),commandRequest.getReaderUserId(),commandRequest.getLastReadMessageId());

		ReadPositionUpdateResponseDTO readMsgResponse = GrpcToDtoConverter
				.convertGrpcToReadPosUpdateResDto(chatOrcStub.readChatMessage(commandRequest));

		log.info("gate-gRPC Read 응답 = rom:{} usr:{} old:{} new:{}", readMsgResponse.getRoomId(), command.getReaderUserId(), readMsgResponse
				.getOldLastReadMessageId(), readMsgResponse.getLastReadMessageId());

		return readMsgResponse;
	}
}