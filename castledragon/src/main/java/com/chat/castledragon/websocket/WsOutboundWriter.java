package com.chat.castledragon.websocket;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.chat.castledragon.domain.WebSocketDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class WsOutboundWriter {

	private final ObjectMapper objectMapper; // "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" 오류 막기 위해.

	private final WsSessionRegistry wsSessionRegistry;

	// 생성자 주입
	public WsOutboundWriter(WsSessionRegistry wsSessionRegistry, ObjectMapper objectMapper) {
		this.wsSessionRegistry = wsSessionRegistry;
		this.objectMapper = objectMapper;
	}

	//	====== broadcast ===========================================================================================================
	void broadcastToRoom(Long roomId, String type, Object payloadData, String requestId) throws Exception {
		Map<Long, WebSocketSession> sessions = wsSessionRegistry.roomSessions.get(roomId);

		if (sessions == null || sessions.isEmpty()) {
			log.info("{}번방 broadcast 대상 없음", roomId);
			return;
		}

		sessions.values().removeIf(socketSession -> !socketSession.isOpen());

		WebSocketDTO event = new WebSocketDTO();
		event.setRequestId(requestId);
		event.setWsType(type);
		event.setIsSuccess(true);
		event.setPayload(objectMapper.valueToTree(payloadData));

		String payload = objectMapper.writeValueAsString(event);

		for (WebSocketSession s : sessions.values()) {
			try {
				if (s.isOpen()) {
					s.sendMessage(new TextMessage(payload));
				}
			} catch (Exception e) {
				log.error("broadcast 실패", e);
			}
		}
	}

	void broadcastToRoomExceptUser(Long roomId, String type, Object payloadData, String requestId, Long excludedUserId) throws Exception {
		Map<Long, WebSocketSession> sessions = wsSessionRegistry.roomSessions.get(roomId);

		if (sessions == null || sessions.isEmpty()) {
			log.info("{}번방 typing broadcast 대상 없음", roomId);
			return;
		}

		sessions.values().removeIf(socketSession -> !socketSession.isOpen());

		WebSocketDTO event = new WebSocketDTO();
		event.setRequestId(requestId);
		event.setWsType(type);
		event.setIsSuccess(true);
		event.setPayload(objectMapper.valueToTree(payloadData));

		String payload = objectMapper.writeValueAsString(event);

		for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
			Long userId = entry.getKey();
			WebSocketSession s = entry.getValue();

			if (Objects.equals(userId, excludedUserId)) { // userId.equals(excludedUserId) --> userId == null 일 경우 터질 수 있숨.
				continue;
			}

			try {
				if (s.isOpen()) {
					s.sendMessage(new TextMessage(payload));
				}
			} catch (Exception e) {
				log.error("typing broadcast 실패", e);
			}
		}
	}

	//	====== Success 응답 보내기 ===========================================================================================================
	void responseOk(WebSocketSession session, WebSocketDTO request, String wsType, Object payload) throws Exception {
		WebSocketDTO response = new WebSocketDTO();

		response.setRequestId(request.getRequestId());
		response.setWsType(wsType);
		response.setIsSuccess(true);
		response.setPayload(objectMapper.valueToTree(payload));

		dispatchToSession(session, response);
	}

	//	====== Fail 응답 보내기 ===========================================================================================================
	void responseFail(WebSocketSession session, WebSocketDTO request, String wsType, String errorMessage) throws Exception {
		WebSocketDTO response = new WebSocketDTO();

		response.setRequestId(request.getRequestId());
		response.setWsType(wsType);
		response.setIsSuccess(false);
		response.setWsMessage(errorMessage);

		dispatchToSession(session, response);
	}

	//	====== 응답 Session에 보내기 ===========================================================================================================
	void dispatchToSession(WebSocketSession session, WebSocketDTO dto) throws Exception {
		if (!session.isOpen()) {
			log.info("responseToSession is Not Open");
			return;
		}

		String payload = objectMapper.writeValueAsString(dto);
		session.sendMessage(new TextMessage(payload));
	}

}
