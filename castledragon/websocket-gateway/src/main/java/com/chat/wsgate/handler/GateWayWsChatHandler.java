package com.chat.wsgate.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.command.ReadChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ReadPositionUpdateResponseDTO;
import com.chat.contract.domain.SessionUserDTO;
import com.chat.contract.domain.WebSocketDTO;
import com.chat.wsgate.auth.WsAuth;
import com.chat.wsgate.client.ChatOrchestratorClient;
import com.chat.wsgate.domain.PayloadReadChatMessageRequestDTO;
import com.chat.wsgate.domain.PayloadSendChatMessageRequestDTO;
import com.chat.wsgate.domain.PayloadTypingRequestDTO;
import com.chat.wsgate.domain.PayloadTypingResponseDTO;
import com.chat.wsgate.outbound.GateWayWsOutboundWriter;
import com.chat.wsgate.support.WsPayloadConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class GateWayWsChatHandler {
	private final WsAuth wsAuth;
	private final GateWayWsOutboundWriter gwWsOutboundWriter;

	private final WsPayloadConverter wsPayloadConverter;

	private final ChatOrchestratorClient chatOrcClient;

	//	====== 메세지 전송 ===========================================================================================================
	public void handleSendMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		PayloadSendChatMessageRequestDTO payload = wsPayloadConverter.convert(dto, PayloadSendChatMessageRequestDTO.class);

		if (payload.getRoomId() == null || payload.getMessageText() == null) {
			log.warn("SEND_MSG Data 누락 : {} / {}", payload.getRoomId(), payload.getMessageText());
			gwWsOutboundWriter.responseFail(session, dto, "MSG_SEND_FAIL", "SEND_MSG 필수값 누락");
			return;
		}

		try {
			// Command 생성. 최적화를 위해 me에서 publicId까지 꺼내고 함께 보낸다. orc에서 userId로 publicId를 DB에서 굳이 또 하는 것보다 좋음. session값이라 신뢰 가능.
			CreateChatMessageCommand createChtMsgCmd = new CreateChatMessageCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload
					.getMessageText());

			ChatMessageViewDTO chatResponse = chatOrcClient.createChatMessage(createChtMsgCmd);

			gwWsOutboundWriter.broadcastToRoom(payload.getRoomId(), "MSG_CREATED", chatResponse, dto.getRequestId()); // chatService.sendMessage()가 성공했을 때만 broadcast해야 하니까. try{}안에 두어라.
			log.info("{}번유저 -> {}번방 sendMsg : {}", me.getUserId(), payload.getRoomId(), payload.getMessageText());

		} catch (Exception e) {
			log.error("메시지 저장 실패", e);
			gwWsOutboundWriter.responseFail(session, dto, "MSG_SEND_FAIL", "메시지 저장 실패");
			return;
		}
	}

	//	====== 메세지 읽기 ===========================================================================================================
	public void handleReadMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		//		PayloadReadMessageDTO payload = new PayloadReadMessageDTO(); <--- @NoArgsConstructor가 없어서, 이 부분에서 터져버리는거다. 코드는 딱 1줄이라 티가나질않아서 찾기 빡셈.@NoArgsConstructor 넣고, req res 나누고 코드 쫌만 바꿨는데, 채팅 urc문제 다 해결함;;ㄷ;; 뭐야;
		//		payload.setRoomId(1L);
		//		payload.setLastReadMessageId(6L);
		//		--->
		PayloadReadChatMessageRequestDTO payload = wsPayloadConverter.convert(dto, PayloadReadChatMessageRequestDTO.class); // ws내부의 payload를 꺼낸다.

		if (payload.getRoomId() == null || payload.getLastReadMessageId() == null) {
			log.info("readMsg 필수 값 누락 : {} / {}", payload.getRoomId(), payload.getLastReadMessageId());
			gwWsOutboundWriter.responseFail(session, dto, "READ_MSG_FAIL", "READ_MSG 필수값 누락");
			return;
		}

		try {
			ReadChatMessageCommand readChtMsgCmd = new ReadChatMessageCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload
					.getLastReadMessageId());

			ReadPositionUpdateResponseDTO readPositionResponse = chatOrcClient.readChatMessage(readChtMsgCmd);

			if (!Boolean.TRUE.equals(readPositionResponse.getUpdated())) {
				return;
				//				throw new IllegalArgumentException("nono Boolean false."); // ,조용히 return 시켜야함.
			}

			gwWsOutboundWriter.broadcastToRoom(payload.getRoomId(), "MSG_READ", readPositionResponse, dto.getRequestId());
		} catch (Exception e) {
			log.error("READ_MSG 읽음 처리 실패", e);
			gwWsOutboundWriter.responseFail(session, dto, "READ_MSG_FAIL", "읽음처리실패");
		}

	}

	//	====== 채팅 입력 이벤트 start/stop ===========================================================================================================
	public void handleTyping(WebSocketSession session, WebSocketDTO dto, String eventType) throws Exception {
		//		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		//		PayloadTypingRequestDTO payload = convertPayload(dto, PayloadTypingRequestDTO.class);
		PayloadTypingRequestDTO payload = wsPayloadConverter.convert(dto, PayloadTypingRequestDTO.class);

		if (payload.getRoomId() == null) {
			log.warn("타이핑 데이터 중 roomId null");
			gwWsOutboundWriter.responseFail(session, dto, eventType + "_FAIL", "TYPING 필수값 누락");
			return;
		}

		PayloadTypingResponseDTO responsePayload = new PayloadTypingResponseDTO(payload.getRoomId(), me.getPublicId(), me.getNickname());

		//		log.info("{} in room={}", eventType, responsePayload.getRoomId());

		gwWsOutboundWriter.broadcastToRoomExceptUser(payload.getRoomId(), eventType, responsePayload, dto.getRequestId(), me.getUserId());
	}
}
