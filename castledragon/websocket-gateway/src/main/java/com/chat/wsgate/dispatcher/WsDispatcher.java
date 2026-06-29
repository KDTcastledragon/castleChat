package com.chat.wsgate.dispatcher;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.chat.contract.domain.SessionUserDTO;
import com.chat.contract.domain.WebSocketDTO;
import com.chat.wsgate.handler.GateWayWsChatHandler;
import com.chat.wsgate.handler.GateWayWsConnectionHandler;
import com.chat.wsgate.handler.GatewayWsRoomHandler;
import com.chat.wsgate.outbound.GateWayWsOutboundWriter;
import com.chat.wsgate.session.WsSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class WsDispatcher extends TextWebSocketHandler { // Ws 최상위 입구.

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final WsSessionRegistry wsSessionRegistry;
	private final GateWayWsOutboundWriter gwWsOutboundWriter;

	private final GatewayWsRoomHandler gwWsRoomHandler;
	private final GateWayWsConnectionHandler gwWsConnectionHandler;
	private final GateWayWsChatHandler gwWsChatHandler;

	//	====== 연결 이후 메소드 ===========================================================================================================
	@Override
	public void afterConnectionEstablished(WebSocketSession session) { // 이게 실행되는 순간 = 클라이언트가 ws 연결 성공한 순간
		log.info("ws 연결 성공 : WSid▶{}   uri▶{}", session.getId(), session.getUri());
	}

	//	====== 메세지 관리 Dispatcher ===========================================================================================================
	// TextWebSocketHandler안에 handleTextMessage내장 메소드 존재. 그래서 @Override 붙임. 반드시 약속된 메서드인 handleTextMessage를 입구로 써야 합니다.
	@Override // --> 부모 클래스에 이미 있는 메서드를 내가 원하는 방식으로 다시 작성한다.
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		WebSocketDTO dto = null;

		try {
			//			log.info("WebSocket_msg 도착 : {}", message.getPayload());

			//		ChatDTO dto = objectMapper.readValue(message.getPayload(), ChatDTO.class); // JSON 문자열을 ChatDTO 객체로 바꿔라 --> WebSocketDTO로 변경되어 legacy.
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
			case "LEFT_ROOM" -> gwWsRoomHandler.handleLeftRoom(session, dto);
			case "TYPING_START" -> gwWsChatHandler.handleTyping(session, dto, "TYPING_START");
			case "TYPING_STOP" -> gwWsChatHandler.handleTyping(session, dto, "TYPING_STOP");
			case "SEND_MSG" -> gwWsChatHandler.handleSendMessage(session, dto);
			case "READ_MSG" -> gwWsChatHandler.handleReadMessage(session, dto);
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

	// ====== ws 연결 종료 ===========================================================================================================
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		wsSessionRegistry.removeSessionAllRooms(session);
		SessionUserDTO connectedUser = wsSessionRegistry.removeSessionAllRooms(session); // remove : key에 해당하는 entry를 삭제하면서, 해당 entry의 value를 반환한다.

		if (connectedUser != null) {
			log.info("{}-({})님이 종료하였습니다.", connectedUser.getNickname(), connectedUser.getUserId());
		} else {
			log.info("식별되지 않은 WS 종료. Wsid▶ {}", session.getId());
		}

		//		roomSessions.forEach((roomId, userMap) -> {
		//
		//			userMap.entrySet().removeIf(entry -> entry.getValue().equals(session));
		//
		//			// 방이 비었으면 room 자체 제거
		//			if (userMap.isEmpty()) {
		//				roomSessions.remove(roomId);
		//			}
		//		});

		wsSessionRegistry.removeSessionAllRooms(session);

		gwWsOutboundWriter.removeSessionLock(session);

		log.info("ws 연결 종료 : WSid▶{}   status▶{}", session.getId(), status);
	} // afterConnectionClosed 끝.

	// ====== ws 연결 강제 종료 (로그아웃) ===========================================================================================================
	public void closeUserWebSocketConnection(Long userId) {
		WebSocketSession targetSession = wsSessionRegistry.findSessionByUserId(userId);
		// entrySet() : Map 안에 들어있는 key-value 한 쌍들을 전부 꺼내서 볼 수 있게 해주는 것
		//		for (Map.Entry<WebSocketSession, SessionUserDTO> entry : wsSessionRegistry.connectedUserSessions.entrySet()) {
		//			SessionUserDTO connectedUser = entry.getValue();
		//
		//			if (Objects.equals(connectedUser.getUserId(), userId)) {
		//				targetSession = entry.getKey();
		//				break;
		//			}
		//		}

		if (targetSession == null) {
			log.info("로그아웃 WS 대상 없음 userId={}", userId);
			return;
		}

		//		allExitRooms(targetSession);
		//		connectedUserSessions.remove(targetSession);
		// ---> afterConnectionClosed의 중복을 피하기 위해, 주석처리로 제거.
		// -->  로그아웃 API에서는 WebSocketSession을 직접 정리하지 않고 close만 호출했습니다. 
		//		실제 세션 제거와 roomSessions 정리는 WebSocket lifecycle callback인 afterConnectionClosed에서 단일하게 처리하도록 했습니다. 
		//		이렇게 해서 브라우저 종료, 네트워크 끊김, 로그아웃 등 모든 종료 경로의 cleanup이 한 곳으로 모이게 했습니다.

		if (targetSession.isOpen()) {
			try {
				targetSession.close(CloseStatus.NORMAL);
			} catch (Exception e) {
				log.error("로그아웃 WS 종료 실패 userId={}", userId, e);
			}
		}
	}// closeUserSession

} // ChatHandler 끝.
