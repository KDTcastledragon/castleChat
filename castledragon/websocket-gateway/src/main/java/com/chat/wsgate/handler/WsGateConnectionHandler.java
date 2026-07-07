package com.chat.wsgate.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.user.domain.SessionUserDTO;
import com.chat.contract.websocket.domain.WebSocketDTO;
import com.chat.wsgate.auth.WsGateAuth;
import com.chat.wsgate.outbound.WsGateOutboundWriter;
import com.chat.wsgate.session.WsGateSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class WsGateConnectionHandler {
	private final WsGateAuth wsGateAuth;
	private final WsGateSessionRegistry wsGateSessionRegistry;

	private final WsGateOutboundWriter wsGateOutboundWriter;

	//	====== 유저 접속 ============================================================================================================
	public void handleConnectUser(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO loginUser = wsGateAuth.getLoginUser(session);

		if (loginUser == null) {
			log.warn("인증되지 않은 WS CONNECT 요청. WSid={}", session.getId());
			wsGateOutboundWriter.responseFail(session, dto, "CONNECT_USER_FAIL", "로그인이 필요합니다.");

			if (session.isOpen()) {
				session.close(CloseStatus.NOT_ACCEPTABLE);
			}

			return;
		}

		//		wsSessionRegistry.connectedUserSessions.put(session, myUserId);
		wsGateSessionRegistry.registerConnectedUser(session, loginUser);

		log.info("{}-({})님이 접속하셨습니다.", loginUser.getNickname(), loginUser.getUserId());
		wsGateOutboundWriter.responseOk(session, dto, "CONNECT_USER_OK", loginUser);

	}// handleConnectUser

	// ====== 로그아웃 (연결 강제 종료) ===========================================================================================================
	public void closeUserWebSocketConnection(Long userId) {
		WebSocketSession targetSession = wsGateSessionRegistry.findSessionByUserId(userId);

		if (targetSession == null) {
			log.info("로그아웃 WS 대상 없음 userId={}", userId);
			return;
		}

		if (!targetSession.isOpen()) {
			log.info("이미 닫힌 {}님의 WS.", userId);
			return;
		} else {
			try {
				targetSession.close(CloseStatus.NORMAL);
				log.info("{}님 WS로그아웃.", userId);
			} catch (Exception e) {
				log.error("{}님 WS 로그아웃 요청 실패. err: {}", userId, e);
			}
		}// else
	}// closeUserSession
}


