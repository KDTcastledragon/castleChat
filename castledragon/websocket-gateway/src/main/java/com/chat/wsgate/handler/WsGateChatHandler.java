package com.chat.wsgate.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.command.chatting.CreateChatMessageCommand;
import com.chat.contract.command.chatting.DeleteChatMessageCommand;
import com.chat.contract.command.chatting.ReactChatMessageCommand;
import com.chat.contract.command.chatting.ReadChatMessageCommand;
import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.DeleteChatMessageResponseDTO;
import com.chat.contract.domain.chatting.ReactChatMessageEventResponseDTO;
import com.chat.contract.domain.chatting.ReadPositionUpdateResponseDTO;
import com.chat.contract.domain.user.SessionUserDTO;
import com.chat.contract.domain.websocket.WebSocketDTO;
import com.chat.wsgate.auth.WsGateAuth;
import com.chat.wsgate.client.WsGateChatOrchestratorClient;
import com.chat.wsgate.domain.chatting.PayloadDeleteChatMessageRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadReactChatMessageRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadReadChatMessageRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadSendChatMessageRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadTypingRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadTypingResponseDTO;
import com.chat.wsgate.outbound.WsGateOutboundWriter;
import com.chat.wsgate.support.WsGatePayloadConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class WsGateChatHandler {
	private final WsGateAuth wsGateAuth;
	private final WsGateOutboundWriter wsGateOutboundWriter;

	private final WsGatePayloadConverter wsGatePayloadConverter;

	private final WsGateChatOrchestratorClient wsGateChatOrcClient;

	// 1.로그인 검증  2.payload 꺼내기  3.data누락 검증  4.커맨드 생성  5.grpc 커맨드 input + grpc output 받기  6.output broadcast 

	//	====== 메세지 전송 ===========================================================================================================
	public void handleSendMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadSendChatMessageRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadSendChatMessageRequestDTO.class);

		if (payload.getRoomId() == null || payload.getMessageText() == null) {
			log.warn("SEND_MSG Data 누락 : {} / {}", payload.getRoomId(), payload.getMessageText());
			wsGateOutboundWriter.responseFail(session, dto, "SEND_MSG_FAIL", "SEND_MSG 필수값 누락");
			return;
		}

		try {
			// Command 생성. 최적화를 위해 me에서 publicId까지 꺼내고 함께 보낸다. orc에서 userId로 publicId를 DB에서 굳이 또 하는 것보다 좋음. session값이라 신뢰 가능.
			CreateChatMessageCommand createChtMsgCmd = new CreateChatMessageCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload
					.getMessageType(), payload.getMessageText(), payload.getReplyToMessageId(), payload.getAttachmentIds());

			ChatMessageViewResponseDTO grpcResponse = wsGateChatOrcClient.createChatMessage(createChtMsgCmd);

			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_CREATED", grpcResponse, dto.getRequestId()); // chatService.sendMessage()가 성공했을 때만 broadcast해야 하니까. try{}안에 두어라.
			log.info("{}번유저 -> {}번방 sendMsg : {}", me.getUserId(), grpcResponse.getRoomId(), grpcResponse.getMessageText());

		} catch (Exception e) {
			log.error("SEND_MSG 예외처리발생", e);
			wsGateOutboundWriter.responseFail(session, dto, "MSG_SEND_FAIL", "SEND_MSG 예외처리발생");
			return;
		}
	}

	//	====== 메세지 읽기 ===========================================================================================================
	public void handleReadMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		//		PayloadReadMessageDTO payload = new PayloadReadMessageDTO(); <--- @NoArgsConstructor가 없어서, 이 부분에서 터져버리는거다. 코드는 딱 1줄이라 티가나질않아서 찾기 빡셈.@NoArgsConstructor 넣고, req res 나누고 코드 쫌만 바꿨는데, 채팅 urc문제 다 해결함;;ㄷ;; 뭐야;
		//		payload.setRoomId(1L);
		//		payload.setLastReadMessageId(6L);
		//		--->
		PayloadReadChatMessageRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadReadChatMessageRequestDTO.class); // ws내부의 payload를 꺼낸다.

		if (payload.getRoomId() == null || payload.getLastReadMessageId() == null) {
			log.warn("readMsg 필수 값 누락 : {} / {}", payload.getRoomId(), payload.getLastReadMessageId());
			wsGateOutboundWriter.responseFail(session, dto, "READ_MSG_FAIL", "READ_MSG 필수값 누락");
			return;
		}

		try {
			ReadChatMessageCommand readChtMsgCmd = new ReadChatMessageCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload
					.getLastReadMessageId());

			ReadPositionUpdateResponseDTO grpcResponse = wsGateChatOrcClient.readChatMessage(readChtMsgCmd);

			Boolean updatedLog4j2 = grpcResponse == null ? null : grpcResponse.getUpdated(); // log 확인용.

			if (!Boolean.TRUE.equals(grpcResponse.getUpdated())) {
				log.info("[false]gate-ChatHandler.읽음처리 응답 updated : {}", updatedLog4j2);
				return;
				//				throw new IllegalArgumentException("nono Boolean false."); // ,조용히 return 시켜야함.
			}

			log.info("[true]gate-ChatHandler.읽음처리 응답 : {}", grpcResponse);

			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_READ", grpcResponse, dto.getRequestId());
		} catch (Exception e) {
			log.error("READ_MSG 예외처리발생: {}", e);
			wsGateOutboundWriter.responseFail(session, dto, "READ_MSG_FAIL", "READ_MSG 예외처리발생");
		}

	}

	//	====== 채팅 입력 이벤트 start/stop ===========================================================================================================
	public void handleTyping(WebSocketSession session, WebSocketDTO dto, String eventType) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadTypingRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadTypingRequestDTO.class);

		if (payload.getRoomId() == null) {
			log.warn("타이핑 데이터 중 roomId null");
			wsGateOutboundWriter.responseFail(session, dto, eventType + "_FAIL", "TYPING 필수값 누락");
			return;
		}

		PayloadTypingResponseDTO responsePayload = new PayloadTypingResponseDTO(payload.getRoomId(), me.getPublicId(), me.getNickname());

		//		log.info("{} in room={}", eventType, responsePayload.getRoomId());

		wsGateOutboundWriter.broadcastToRoomExceptUser(payload.getRoomId(), eventType, responsePayload, dto.getRequestId(), me.getUserId());
	}

	// ====== 메시지 삭제 ====================================================================================================================
	public void handleDeleteMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadDeleteChatMessageRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadDeleteChatMessageRequestDTO.class);

		if (payload.getRoomId() == null || payload.getMessageId() == null) {
			log.warn("delete msg roomId:{} msgId:{} 누락", payload.getRoomId(), payload.getMessageId());
			wsGateOutboundWriter.responseFail(session, dto, "DELETE_MSG_FAIL", "roomId,msgId 누락");
			return;
		}

		try {
			DeleteChatMessageCommand deleteChtMsgCmd = new DeleteChatMessageCommand(payload.getRoomId(), payload.getMessageId(), me.getUserId(), me
					.getPublicId());

			DeleteChatMessageResponseDTO grpcResponse = wsGateChatOrcClient.deleteChatMessage(deleteChtMsgCmd);

			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_DELETED", grpcResponse, dto.getRequestId());
		} catch (Exception e) {
			log.error("MSG_DELETED 예외처리발생: {}", e);
			wsGateOutboundWriter.responseFail(session, dto, "MSG_DELETED_FAIL", "MSG_DELETED 예외처리발생");
		}
	}

	// ====== 메시지 리액션 ====================================================================================================================
	public void handleReactMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadReactChatMessageRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadReactChatMessageRequestDTO.class);

		if (payload.getRoomId() == null || payload.getMessageId() == null || payload.getReactionType() == null || payload.getReactionCode() == null
				|| payload.getAddRequested() == null) {
			log.warn("MSG_REACTION_UPDATED 필수값 누락 : roomId:{} msgId:{} type:{} code:{} addReq:{}", payload.getRoomId(), payload
					.getMessageId(), payload.getReactionType(), payload.getReactionCode(), payload.getAddRequested());
			wsGateOutboundWriter.responseFail(session, dto, "REACT_MSG_FAIL", "주요값 누락.");
			return;
		}

		try {
			ReactChatMessageCommand reactChtMsgCmd = new ReactChatMessageCommand(payload.getRoomId(), payload.getMessageId(), me.getUserId(), me
					.getPublicId(), payload.getReactionType(), payload.getReactionCode(), payload.getAddRequested());

			ReactChatMessageEventResponseDTO grpcResponse = wsGateChatOrcClient.reactChatMessage(reactChtMsgCmd);

			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_REACTION_UPDATED", grpcResponse, dto.getRequestId());
		} catch (Exception e) {
			log.error("MSG_REACTION_UPDATED 예외처리발생: {}", e);
			wsGateOutboundWriter.responseFail(session, dto, "MSG_REACTION_UPDATED_FAIL", "MSG_REACTION_UPDATED 예외처리발생");
		}
	}

}
