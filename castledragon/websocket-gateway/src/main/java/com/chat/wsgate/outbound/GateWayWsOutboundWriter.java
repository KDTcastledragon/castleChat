package com.chat.wsgate.outbound;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.domain.WebSocketDTO;
import com.chat.wsgate.session.GateWayWsSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class GateWayWsOutboundWriter {

	private final ObjectMapper objectMapper; // "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" 오류 막기 위해.

	private final GateWayWsSessionRegistry gateWayWsSessionRegistry;

	//	private final ChatMetrics chatMetrics;

	// TEXT_PARTIAL_WRITING 해결용. 같은 WebSocketSession에 동시에 sendMessage가 들어가는 것을 막기 위한 session별 lock.
	private final Map<String, Object> sessionSendLocks = new ConcurrentHashMap<>();

	private Object getSessionSendLock(WebSocketSession session) {
		return sessionSendLocks.computeIfAbsent(session.getId(), id -> new Object());
	}

	public void removeSessionLock(WebSocketSession session) {
		if (session == null) {
			return;
		}
		sessionSendLocks.remove(session.getId());
	}

	private void sendSafely(WebSocketSession session, String payload) throws Exception {
		if (session == null) {
			return;
		}

		synchronized (getSessionSendLock(session)) {
			if (!session.isOpen()) {
				log.info("WebSocketSession is not open. sessionId={}", session.getId());
				return;
			}

			session.sendMessage(new TextMessage(payload));
		}
	}

	//	====== broadcast ===========================================================================================================
	public void broadcastToRoom(Long roomId, String type, Object payloadData, String requestId) throws Exception {
		Map<Long, WebSocketSession> sessions = gateWayWsSessionRegistry.getRoomSessions(roomId);

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

		for (WebSocketSession sess : sessions.values()) {
			try {

				sendSafely(sess, payload);

				//	if (sess.isOpen()) {
				//		sess.sendMessage(new TextMessage(payload));
				//	}
			} catch (Exception e) {
				log.error("broadcast 실패", e);
			}
		}
	}

	public void broadcastToRoomExceptUser(Long roomId, String type, Object payloadData, String requestId, Long excludedUserId) throws Exception {
		Map<Long, WebSocketSession> sessions = gateWayWsSessionRegistry.getRoomSessions(roomId);

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
			WebSocketSession sess = entry.getValue();

			if (Objects.equals(userId, excludedUserId)) { // userId.equals(excludedUserId) --> userId == null 일 경우 터질 수 있숨.
				continue;
			}

			try {
				sendSafely(sess, payload);

				// ======< legacy ver1 : singleProcess & lock 적용 >================================
				//	synchronized (getSessionSendLock(sess)) {
				//		if (sess.isOpen()) {
				//			sess.sendMessage(new TextMessage(payload));
				//		}
				//	}

				// ======< legacy ver0 : singleProcess >===========================================
				//	if (s.isOpen()) {
				//		s.sendMessage(new TextMessage(payload));
				//	}
			} catch (Exception e) {
				log.error("typing broadcast 실패", e);
			}
		}
	}

	//	====== Success 응답 보내기 ===========================================================================================================
	public void responseOk(WebSocketSession session, WebSocketDTO request, String wsType, Object payload) throws Exception {
		WebSocketDTO response = new WebSocketDTO();

		response.setRequestId(request.getRequestId());
		response.setWsType(wsType);
		response.setIsSuccess(true);
		response.setPayload(objectMapper.valueToTree(payload));

		dispatchToSession(session, response);
	}

	//	====== Fail 응답 보내기 ===========================================================================================================
	public void responseFail(WebSocketSession session, WebSocketDTO request, String wsType, String errorMessage) throws Exception {
		WebSocketDTO response = new WebSocketDTO();

		response.setRequestId(request.getRequestId());
		response.setWsType(wsType);
		response.setIsSuccess(false);
		response.setWsMessage(errorMessage);

		dispatchToSession(session, response);
	}

	//	====== 응답 Session에 보내기 ===========================================================================================================
	public void dispatchToSession(WebSocketSession session, WebSocketDTO dto) throws Exception {
		String payload = objectMapper.writeValueAsString(dto);
		sendSafely(session, payload);

		//		if (!session.isOpen()) {
		//			log.info("responseToSession is Not Open");
		//			return;
		//		}
		//
		//		String payload = objectMapper.writeValueAsString(dto);
		//
		//		synchronized (getSessionSendLock(session)) {
		//			session.sendMessage(new TextMessage(payload));
		//		}
	}

}
