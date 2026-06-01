package com.chat.castledragon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.chat.castledragon.websocket.WsDispatcher;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer { // WebSocketConfig 이름은 중요하지 않다. 위 2개의 @어노테이션이 중요함.

	private final WsDispatcher wsDispatcher;

	public WebSocketConfig(WsDispatcher wsDispatcher) {
		this.wsDispatcher = wsDispatcher;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

		//		registry.addHandler(wsHandler, "/ws/chat").setAllowedOrigins("*"); // 어떤 Origin에서 온 WebSocket 연결을 허용할 것인가?
		registry.addHandler(wsDispatcher, "/ws/chat").addInterceptors(new HttpSessionHandshakeInterceptor()).setAllowedOriginPatterns("http://localhost:*");
	}

	//	여기서 의미는:
	//
	//		/ws/chat으로 WebSocket 연결이 오면
	//		chatHandler가 처리하게 등록한다
	//		Spring WebSocket 인프라는 대략 이런 역할을 합니다.
	//
	//		1. /ws/chat WebSocket handshake 처리
	//		2. 연결 성공 후 WebSocketSession 생성
	//		3. 텍스트 메시지가 오면 TextWebSocketHandler의 handleTextMessage 호출
	//		4. 연결되면 afterConnectionEstablished 호출
	//		5. 끊기면 afterConnectionClosed 호출
	//		즉 네가 override한 메서드들이 자동 호출되는 이유가 이 인프라 때문입니다.
	//
	//		afterConnectionEstablished(...)
	//		handleTextMessage(...)
	//		afterConnectionClosed(...)
}