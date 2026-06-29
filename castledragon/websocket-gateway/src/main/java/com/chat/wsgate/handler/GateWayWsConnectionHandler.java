package com.chat.wsgate.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.domain.SessionUserDTO;
import com.chat.contract.domain.WebSocketDTO;
import com.chat.wsgate.auth.WsAuth;
import com.chat.wsgate.outbound.GateWayWsOutboundWriter;
import com.chat.wsgate.session.WsSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class GateWayWsConnectionHandler {
	private final WsAuth wsAuth;
	private final WsSessionRegistry wsSessionRegistry;

	private final GateWayWsOutboundWriter gateWayWsOutboundWriter;

	//	====== 유저 접속 ============================================================================================================
	public void handleConnectUser(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO loginUser = wsAuth.getLoginUser(session);

		if (loginUser == null) {
			log.warn("인증되지 않은 WS CONNECT 요청. WSid={}", session.getId());
			gateWayWsOutboundWriter.responseFail(session, dto, "CONNECT_USER_FAIL", "로그인이 필요합니다.");

			if (session.isOpen()) {
				session.close(CloseStatus.NOT_ACCEPTABLE);
			}

			return;
		}

		//		wsSessionRegistry.connectedUserSessions.put(session, myUserId);
		wsSessionRegistry.registerConnectedUser(session, loginUser);

		log.info("{}-({})님이 접속하셨습니다.", loginUser.getNickname(), loginUser.getUserId());
		gateWayWsOutboundWriter.responseOk(session, dto, "CONNECT_USER_OK", loginUser);

	}// handleConnectUser
}
