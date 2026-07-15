package com.chat.wsgate.outbound;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.websocket.domain.WebSocketDTO;
import com.chat.wsgate.session.WsGateSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class WsGateOutboundWriter {

	private final ObjectMapper objectMapper; // "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" 오류 막기 위해.

	private final WsGateSessionRegistry wsGateSessionRegistry;

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

	private WebSocketDTO createSuccessEvent(String type, Object payloadData, String requestId) {
		WebSocketDTO event = new WebSocketDTO();

		event.setRequestId(requestId);
		event.setWsType(type);
		event.setIsSuccess(true);
		event.setPayload(objectMapper.valueToTree(payloadData));

		return event;
	}

	private WebSocketDTO createFailEvent(String type, String errorMessage, String requestId) {
		WebSocketDTO event = new WebSocketDTO();

		event.setRequestId(requestId);
		event.setWsType(type);
		event.setIsSuccess(false);
		event.setWsMessage(errorMessage);

		return event;
	}

	private String serialize(WebSocketDTO event) throws Exception {
		return objectMapper.writeValueAsString(event);
	}

	private void dispatchSerializedToSession(WebSocketSession session, String payload) throws Exception {
		sendSafely(session, payload);
	}

	//	====== broadcast ===========================================================================================================
	public void broadcastToRoom(Long roomId, String type, Object payloadData, String requestId) throws Exception {
		Map<Long, WebSocketSession> sessions = wsGateSessionRegistry.getRoomSessions(roomId);

		if (sessions == null || sessions.isEmpty()) {
			log.info("{}번방 broadcast 대상 없음", roomId);
			return;
		}

		sessions.values().removeIf(socketSession -> !socketSession.isOpen());

		String payload = serialize(createSuccessEvent(type, payloadData, requestId));

		for (WebSocketSession sess : sessions.values()) {
			try {
				dispatchSerializedToSession(sess, payload);
			} catch (Exception e) {
				log.error("broadcast 실패", e);
			}
		}
	}

	public void broadcastToRoomExceptUser(Long roomId, String type, Object payloadData, String requestId, Long excludedUserId) throws Exception {
		Map<Long, WebSocketSession> sessions = wsGateSessionRegistry.getRoomSessions(roomId);

		if (sessions == null || sessions.isEmpty()) {
			log.info("{}번방 typing broadcast 대상 없음", roomId);
			return;
		}

		sessions.values().removeIf(socketSession -> !socketSession.isOpen());

		String payload = serialize(createSuccessEvent(type, payloadData, requestId));

		for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
			Long userId = entry.getKey();
			WebSocketSession sess = entry.getValue();

			if (Objects.equals(userId, excludedUserId)) { // userId.equals(excludedUserId) --> userId == null 일 경우 터질 수 있숨.
				continue;
			}

			try {
				dispatchSerializedToSession(sess, payload);

			} catch (Exception e) {
				log.error("typing broadcast 실패", e);
			}
		}
	}

	//	====== Success 응답 보내기 ===========================================================================================================
	public void responseOk(WebSocketSession session, WebSocketDTO request, String wsType, Object payload) throws Exception {
		dispatchToSession(session, createSuccessEvent(wsType, payload, request.getRequestId()));
	}

	//	====== Fail 응답 보내기 ===========================================================================================================
	public void responseFail(WebSocketSession session, WebSocketDTO request, String wsType, String errorMessage) throws Exception {
		dispatchToSession(session, createFailEvent(wsType, errorMessage, request.getRequestId()));
	}

	//	====== 응답 Session에 보내기 ===========================================================================================================
	public void dispatchToSession(WebSocketSession session, WebSocketDTO dto) throws Exception {
		dispatchSerializedToSession(session, serialize(dto));
	}

	public void pushToSingleUser(Long userId, String type, Object payloadData, String requestId) throws Exception {
		WebSocketSession targetSession = wsGateSessionRegistry.findSessionByUserId(userId);

		if (targetSession == null || !targetSession.isOpen()) {
			log.info("pushToSingleUser 대상 세션 없음. userId={}, type={}", userId, type);
			return;
		}

		dispatchToSession(targetSession, createSuccessEvent(type, payloadData, requestId));
	}

	public void pushToSingleUserByPublicId(String publicId, String type, Object payloadData, String requestId) throws Exception {
		WebSocketSession targetSession = wsGateSessionRegistry.findSessionByPublicId(publicId);

		if (targetSession == null || !targetSession.isOpen()) {
			return;
		}

		dispatchToSession(targetSession, createSuccessEvent(type, payloadData, requestId));
	}

	public void pushToMultipleUsers(Collection<Long> userIds, String type, Object payloadData, String requestId) throws Exception {
		if (userIds == null || userIds.isEmpty()) {
			log.info("pushToMultipleUsers 대상 없음. type={}", type);
			return;
		}

		String payload = serialize(createSuccessEvent(type, payloadData, requestId));

		for (Long userId : userIds) {
			if (userId == null) {
				continue;
			}

			WebSocketSession targetSession = wsGateSessionRegistry.findSessionByUserId(userId);

			if (targetSession == null || !targetSession.isOpen()) {
				log.info("pushToMultipleUsers 대상 세션 없음. userId={}, type={}", userId, type);
				continue;
			}

			try {
				dispatchSerializedToSession(targetSession, payload);
			} catch (Exception e) {
				log.error("pushToMultipleUsers 실패. userId={}, type={}", userId, type, e);
			}
		}
	}

	public void pushToSingleUserExcept(Long userId, String type, Object payloadData, String requestId, Long excludedUserId) throws Exception {
		if (Objects.equals(userId, excludedUserId)) {
			return;
		}

		pushToSingleUser(userId, type, payloadData, requestId);
	}

}
