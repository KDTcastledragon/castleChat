package com.chat.castledragon.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.castledragon.domain.SessionUserDTO;

@Component
public class WsAuth {
	//	로그인 인증 =======================================================
	SessionUserDTO getLoginUser(WebSocketSession session) {
		Object loginUser = session.getAttributes().get("LOGIN_USER");

		if (loginUser instanceof SessionUserDTO user) {
			return user;
		}

		return null;
	}
}// getLoginUser
