package com.chat.wsgate.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.domain.SessionUserDTO;
import com.chat.wsgate.exception.WsAuthException;

@Component
public class WsAuth {

	//	로그인 유저 꺼내기 =======================================================
	public SessionUserDTO getLoginUser(WebSocketSession session) {
		Object loginUser = session.getAttributes().get("LOGIN_USER");

		if (loginUser instanceof SessionUserDTO user) {
			return user;
		}

		return null;
	}

	// ==== 로그인 user_id =======================================================
	public Long getMyUserIdInWsSession(WebSocketSession session) {
		return requireLoginUser(session).getUserId();
	}

	// ==== 로그인 PK id =======================================================
	public SessionUserDTO requireLoginUser(WebSocketSession session) {
		SessionUserDTO me = getLoginUser(session);

		if (me == null) {
			throw new WsAuthException("로그인이 필요합니다.");
		}

		return me;
	}

}// getLoginUser
