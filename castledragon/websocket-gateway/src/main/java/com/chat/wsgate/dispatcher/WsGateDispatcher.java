package com.chat.wsgate.dispatcher;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.chat.contract.domain.user.SessionUserDTO;
import com.chat.contract.domain.websocket.WebSocketDTO;
import com.chat.wsgate.handler.WsGateChatHandler;
import com.chat.wsgate.handler.WsGateConnectionHandler;
import com.chat.wsgate.handler.WsGateRoomHandler;
import com.chat.wsgate.outbound.WsGateOutboundWriter;
import com.chat.wsgate.session.WsGateSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class WsGateDispatcher extends TextWebSocketHandler { // Ws 최상위 입구.

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final WsGateSessionRegistry wsGateSessionRegistry;
	private final WsGateOutboundWriter gwWsOutboundWriter;

	private final WsGateRoomHandler gwWsRoomHandler;
	private final WsGateConnectionHandler gwWsConnectionHandler;
	private final WsGateChatHandler gwWsChatHandler;

	//	====== 메세지 관리 Dispatcher ===========================================================================================================
	// TextWebSocketHandler안에 handleTextMessage내장 메소드 존재. 그래서 @Override 붙임. 반드시 약속된 메서드인 handleTextMessage를 입구로 써야 합니다.
	@Override // --> 부모 클래스에 이미 있는 메서드를 내가 원하는 방식으로 다시 작성한다.
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		WebSocketDTO dto = null;

		try {
			dto = objectMapper.readValue(message.getPayload(), WebSocketDTO.class);

			if (dto.getWsType() == null) {
				log.warn("WebSocket type 없음: {}", message.getPayload());
				return;
			}

			switch (dto.getWsType()) {
			case "CONNECT_USER" -> gwWsConnectionHandler.handleConnectUser(session, dto);

			case "ENTER_ROOM" -> gwWsRoomHandler.handleEnterRoom(session, dto);
			case "ENTER_GROUP_ROOM" -> gwWsRoomHandler.handleEnterRoom(session, dto);
			case "EXIT_ROOM" -> gwWsRoomHandler.handleExitRoom(session, dto);
			case "LEFT_ROOM" -> gwWsRoomHandler.handleExitRoom(session, dto); // 구현해야함
			case "INVITE_ROOM" -> gwWsRoomHandler.handleExitRoom(session, dto); // 구현해야함
			case "TYPING_START" -> gwWsChatHandler.handleTyping(session, dto, "TYPING_START");
			case "TYPING_STOP" -> gwWsChatHandler.handleTyping(session, dto, "TYPING_STOP");
			case "SEND_MESSAGE" -> gwWsChatHandler.handleSendMessage(session, dto);
			case "READ_MESSAGE" -> gwWsChatHandler.handleReadMessage(session, dto);

			case "DELETE_MESSAGE" -> gwWsChatHandler.handleReadMessage(session, dto); // 구현해야함
			case "REACT_MESSAGE" -> gwWsChatHandler.handleReadMessage(session, dto); // 구현해야함
			case "APPLY_ROOM_NOTICE" -> gwWsRoomHandler.handleEnterRoom(session, dto); // 구현해야함

			//		case "LEAVE_ROOM" -> handleLeaveRoom(session, dto);
			default -> {
				log.warn("알 수 없는 WS TYPE : {}", dto.getWsType());
				gwWsOutboundWriter.responseFail(session, dto, "UNKNOWN_TYPE", "알 수 없는 WS TYPE");
			}// default

			}// switch-case

		} catch (Exception e) {
			log.error("WebSocket 메시지 처리 실패: {}", message.getPayload(), e);

			if (dto != null && session.isOpen()) {
				gwWsOutboundWriter.responseFail(session, dto, "WS_MESSAGE_FAIL", "WebSocket 메시지 처리 실패");
			}
		}// try-catch : 이렇게 하면 잘못된 payload가 와도 서버가 FAIL을 보내고, WebSocket 연결 자체는 최대한 유지할 수 있어.
	} // handleTextMessage 끝.

	//	====== 연결 이후 메소드 ===========================================================================================================
	@Override
	public void afterConnectionEstablished(WebSocketSession session) { // 이게 실행되는 순간 = 클라이언트가 ws 연결 성공한 순간
		//		log.info("ws 연결 성공 : WSid▶{}   uri▶{}", session.getId(), session.getUri());
		log.info("ws 연결 성공 : WSid▶{}", session.getId());
	}

	// ====== ws 연결 종료 ===========================================================================================================
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		SessionUserDTO connectedUser = wsGateSessionRegistry.removeConnectedUser(session); // remove : key에 해당하는 entry를 삭제하면서, 해당 entry의 value를 반환한다.
		wsGateSessionRegistry.removeSessionAllRooms(session);
		gwWsOutboundWriter.removeSessionLock(session);

		if (connectedUser != null) {
			log.info("{}-({})님이 종료하였습니다.", connectedUser.getNickname(), connectedUser.getUserId());
		} else {
			log.info("식별되지 않은 WS 종료. Wsid▶ {}", session.getId());
		}

		log.info("ws 연결 종료 : WSid▶{}   status▶{}", session.getId(), status);
	} // afterConnectionClosed 끝.

} // ChatHandler 끝.
