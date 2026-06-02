package com.chat.castledragon.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.castledragon.domain.SessionUserDTO;

@Component
public class WsAuth {

	//	로그인 유저 꺼내기 =======================================================
	SessionUserDTO getLoginUser(WebSocketSession session) {
		Object loginUser = session.getAttributes().get("LOGIN_USER");

		if (loginUser instanceof SessionUserDTO user) {
			return user;
		}

		return null;
	}

	// ==== 로그인 PK id =======================================================
	Long getMyUserIdInWsSession(WebSocketSession session) {
		return requireLoginUserId(session).getUserId();
	}

	// ==== 로그인 PK id =======================================================
	SessionUserDTO requireLoginUserId(WebSocketSession session) {
		SessionUserDTO me = getLoginUser(session);

		if (me == null) {
			throw new WsAuthException("로그인이 필요합니다.");
		}

		return me;
	}
}// getLoginUser
