package com.chat.wsgate.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.command.SendChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.SessionUserDTO;
import com.chat.contract.domain.WebSocketDTO;
import com.chat.wsgate.auth.WsAuth;
import com.chat.wsgate.domain.PayloadSendChatMessageRequestDTO;
import com.chat.wsgate.domain.PayloadTypingRequestDTO;
import com.chat.wsgate.domain.PayloadTypingResponseDTO;
import com.chat.wsgate.grpc.GrpcChatOrchestratorClient;
import com.chat.wsgate.outbound.GateWayWsOutboundWriter;
import com.chat.wsgate.session.WsSessionRegistry;
import com.chat.wsgate.support.WsPayloadConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class GateWayWsChatHandler {
	private final WsAuth wsAuth;
	private final GateWayWsOutboundWriter gwWsOutboundWriter;

	private final WsSessionRegistry wsSessionRegistry;

	private final WsPayloadConverter wsPayloadConverter;

	private final GrpcChatOrchestratorClient grpcChatOrcClient;

	//	====== 메세지 전송 ===========================================================================================================
	public void handleSendMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		//		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		PayloadSendChatMessageRequestDTO payload = wsPayloadConverter.convert(dto, PayloadSendChatMessageRequestDTO.class);

		if (payload.getRoomId() == null || payload.getMessageText() == null) {
			log.warn("SEND_MSG Data 누락 : {} / {}", payload.getRoomId(), payload.getMessageText());
			gwWsOutboundWriter.responseFail(session, dto, "MSG_SEND_FAIL", "SEND_MSG 필수값 누락");
			return;
		}

		try {
			SendChatMessageCommand sendChtMsgCmd = new SendChatMessageCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload.getMessageText(), dto.getRequestId());

			ChatMessageViewDTO resChat = grpcChatOrcClient.sendMessage(sendChtMsgCmd);

			gwWsOutboundWriter.broadcastToRoom(payload.getRoomId(), "MSG_CREATED", resChat, dto.getRequestId()); // chatService.sendMessage()가 성공했을 때만 broadcast해야 하니까. try{}안에 두어라.
			log.info("{}번유저 -> {}번방 sendMsg : {}", me.getUserId(), payload.getRoomId(), payload.getMessageText());

		} catch (Exception e) {
			log.error("메시지 저장 실패", e);
			gwWsOutboundWriter.responseFail(session, dto, "MSG_SEND_FAIL", "메시지 저장 실패");
			return;
		}
		//		responseOk(session, dto, "SEND_MSG_OK", chat);
	}

	//	====== 메세지 읽기 ===========================================================================================================
	public void handleReadMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		//		PayloadReadMessageDTO payload = new PayloadReadMessageDTO(); <--- @NoArgsConstructor가 없어서, 이 부분에서 터져버리는거다. 코드는 딱 1줄이라 티가나질않아서 찾기 빡셈.@NoArgsConstructor 넣고, req res 나누고 코드 쫌만 바꿨는데, 채팅 urc문제 다 해결함;;ㄷ;; 뭐야;
		//		payload.setRoomId(1L);
		//		payload.setLastReadMessageId(6L);
		//		--->
		PayloadReadChatMessageRequestDTO payload = convertPayload(dto, PayloadReadChatMessageRequestDTO.class); // ws내부의 payload를 꺼낸다.

		if (payload.getRoomId() == null || payload.getLastReadMessageId() == null) {
			log.info("readMsg 필수 값 누락 : {} / {}", payload.getRoomId(), payload.getLastReadMessageId());
			gwWsOutboundWriter.responseFail(session, dto, "READ_MSG_FAIL", "READ_MSG 필수값 누락");
			return;
		}

		//		PayloadReadMessageResponseDTO responsePayload = new PayloadReadMessageResponseDTO(payload.getRoomId(), me.getPublicId(), payload.getLastReadMessageId(), updatedUnreadCount);

		//		List<UpdatedUnreadMessagesDTO> updatedMessages = chatService.readChatMessage(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload.getLastReadMessageId());
		//		PayloadReadMessageResponseDTO responsePayload = new PayloadReadMessageResponseDTO(payload.getRoomId(), me.getPublicId(), payload.getLastReadMessageId(), // 얘 어캐하지?
		//				updatedMessages);

		PayloadReadChatMessageResponseDTO responsePayload = chatService.readChatMessage(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload.getLastReadMessageId());

		//		log.info("{}({}) 유저가 {}번방 {}번 메시지까지 읽음", me.getNickname(), me.getUserId(), payload.getRoomId(), payload.getLastReadMessageId());
		chatMetrics.incrementSendMessage();
		wsOutboundWriter.broadcastToRoom(payload.getRoomId(), "MSG_READ", responsePayload, dto.getRequestId());
		//		responseOk(session, dto, "READ_MSG_OK", payload);
	}

	//	====== typing start/stop ===========================================================================================================
	public void handleTyping(WebSocketSession session, WebSocketDTO dto, String eventType) throws Exception {
		//		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		//		PayloadTypingRequestDTO payload = convertPayload(dto, PayloadTypingRequestDTO.class);
		PayloadTypingRequestDTO payload = convertPayload(dto, PayloadTypingRequestDTO.class);

		if (payload.getRoomId() == null) {
			log.warn("TYPING Data roomId null");
			gwWsOutboundWriter.responseFail(session, dto, eventType + "_FAIL", "TYPING 필수값 누락");
			return;
		}

		PayloadTypingResponseDTO responsePayload = new PayloadTypingResponseDTO(payload.getRoomId(), me.getPublicId(), me.getNickname());

		//		log.info("{} in room={}", eventType, responsePayload.getRoomId());

		gwWsOutboundWriter.broadcastToRoomExceptUser(payload.getRoomId(), eventType, responsePayload, dto.getRequestId(), me.getUserId());
	}
}
