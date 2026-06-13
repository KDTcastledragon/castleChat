package com.chat.castledragon.websocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.chat.castledragon.domain.ChatMessageResponseDTO;
import com.chat.castledragon.domain.PayloadEnterRoomDTO;
import com.chat.castledragon.domain.PayloadExitRoomDTO;
import com.chat.castledragon.domain.PayloadReadMessageRequestDTO;
import com.chat.castledragon.domain.PayloadReadMessageResponseDTO;
import com.chat.castledragon.domain.PayloadSendMessageDTO;
import com.chat.castledragon.domain.PayloadTypingRequestDTO;
import com.chat.castledragon.domain.PayloadTypingResponseDTO;
import com.chat.castledragon.domain.SessionUserDTO;
import com.chat.castledragon.domain.WebSocketDTO;
import com.chat.castledragon.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class WsChatEventHandler {

	private final ObjectMapper objectMapper = new ObjectMapper(); //JackSon 라이브러리 객체. 역할 : JSON 문자열 ↔ Java 객체 변환 === ChatHandler 내부에서 계속 재사용하는 JSON 변환기
	//	private : 이 클래스 안에서만 쓰겠다.  /  final : 한 번 만든 뒤 다른 ObjectMapper로 바꾸지 않겠다. 근데, final이라고 해서 Map 안의 내용이 못 바뀌는 건 아닙니다.Map 객체 자체는 고정이고, Map 내부 내용은 계속 변경 가능
	//	roomSessions.put(...) 또는 roomSessions.remove(...) 얘네는 가능. 하지만, roomSessions = new ConcurrentHashMap<>(); 얘는 불가능.

	private final WsSessionRegistry wsSessionRegistry;
	private final WsOutboundWriter wsOutboundWriter;
	private final WsAuth wsAuth;
	private final ChatService chatService;

	// 생성자 주입
	public WsChatEventHandler(WsSessionRegistry wsSessionRegistry, WsOutboundWriter wsOutboundWriter, WsAuth wsAuth, ChatService chatService) {
		this.wsSessionRegistry = wsSessionRegistry;
		this.wsOutboundWriter = wsOutboundWriter;
		this.wsAuth = wsAuth;
		this.chatService = chatService;
	}

	// chatService를 생성자로 받습니다.
	// Spring이 ChatHandler를 만들 때, 이미 Bean으로 등록된 ChatService를 찾아서 넣어줍니다. 이걸 생성자 주입이라고 합니다.
	// 왜 직접 new ChatServiceImpl() 안 하냐??? Spring이 관리하는 Bean을 써야 하기 때문입니다. 트랜잭션 , AOP , 의존성 관리 , 테스트 , 싱글톤 관리 등을 쓸 수 있기 때문에.
	// 그래서,  private final ChatService chatService = new ChatServiceImpl(); <--- 이런식으로 직접 생성하지 않는다.

	// ======= payload 변환 helper ==================================================
	private <T> T convertPayload(WebSocketDTO dto, Class<T> clazz) {
		return objectMapper.convertValue(dto.getPayload(), clazz); // WebSocketDTO 안의 payload를 각각의 메소드에 알맞게 Payload DTO 타입으로 변환하는 공통 함수.
		// <T> : 이 메서드는 호출할 때 원하는 타입을 받아서, 그 타입으로 변환해서 돌려줄 수 있다.
		// Java는 런타임에 제네릭 타입 정보를 잃는 부분이 있습니다. 그래서 T만 보고는 Jackson이 “무슨 클래스로 바꿔야 하는지” 모릅니다. 
		// 그래서 Class<T> clazz를 이용하여 명시적으로 클래스 정보를 넘깁니다. 즉, 'payload를 어떤 클래스 타입으로 변환할지 알려주는 인자'이다.
		// convertValue : 이미 Java 객체/JsonNode/Map 형태로 존재하는 값을 다른 Java 객체 타입으로 변환해주는 메소드. 이미 객체 형태인 값을 '다른 객체'로 바꿉니다.
		// ws의 payload는 JsonNode입니다. 그래서, dto.getPayload()는 문자열이 아니라 JsonNode이다. --> JsonNode를 EnterRoomPayloadDTO 로 바꾸는 겁니다.
		// Jackson은 사람이 아니라 Java에서 JSON을 다루는 대표 라이브러리 이름입니다. 오타 아니에요.
	}

	//	====== 유저 접속 ============================================================================================================
	void handleConnectUser(WebSocketSession session, WebSocketDTO dto) throws Exception {
		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		SessionUserDTO loginUser = wsAuth.getLoginUser(session);

		if (loginUser == null) {
			log.warn("인증되지 않은 WS CONNECT 요청. WSid={}", session.getId());
			wsOutboundWriter.responseFail(session, dto, "CONNECT_USER_FAIL", "로그인이 필요합니다.");

			if (session.isOpen()) {
				session.close(CloseStatus.NOT_ACCEPTABLE);
			}

			return;
		}

		wsSessionRegistry.connectedUserSessions.put(session, loginUser);

		log.info("{}-({})님이 접속하셨습니다.", loginUser.getNickname(), myUserId);
		wsOutboundWriter.responseOk(session, dto, "CONNECT_USER_OK", loginUser);
		//	    
		//		PayloadConnectUserDTO payload = convertPayload(dto, PayloadConnectUserDTO.class);
		//
		//		if (payload.getUserId() == null || payload.getLoginId() == null) {
		//			log.warn("아이디 없이 접속 경고 : {} / {} ", payload.getUserId(), payload.getLoginId());
		//			responseFail(session, dto, "CONNECT_USER_FAIL", "UserId가 없습니다.");
		//			return;
		//		}
		//
		//		log.info("{}-({})님이 접속하셨습니다.", payload.getLoginId(), payload.getUserId());
		//		connectedUserSessions.put(session, payload);
		//		responseOk(session, dto, "CONNECT_USER_OK", payload);

	}// handleConnectUser

	//	====== 채팅방 입장 ===========================================================================================================
	void handleEnterRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		PayloadEnterRoomDTO payload = convertPayload(dto, PayloadEnterRoomDTO.class);

		if (payload.getRoomId() == null || myUserId == null) {
			log.warn("ENTER_ROOM Data 누락 : {} / {}", payload.getRoomId(), myUserId);
			wsOutboundWriter.responseFail(session, dto, "ENTER_ROOM_FAIL", "roomId 또는 userId가 없습니다.");
			return;
		}

		wsSessionRegistry.roomSessions.computeIfAbsent(payload.getRoomId(), k -> new ConcurrentHashMap<>()).put(myUserId, session);
		log.info("{}번 유저 {}번방 ENTER, roomSession등록", myUserId, payload.getRoomId());
		wsOutboundWriter.responseOk(session, dto, "ENTER_ROOM_OK", payload);

	} // handleEnterRoom 끝.

	//	====== 채팅방 닫기 ===========================================================================================================
	void handleExitRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		Long myUserId = wsAuth.getMyUserIdInWsSession(session);

		PayloadExitRoomDTO payload = convertPayload(dto, PayloadExitRoomDTO.class);

		if (payload.getRoomId() == null) {
			wsOutboundWriter.responseFail(session, dto, "EXIT_ROOM_FAIL", "roomId가 없습니다.");
			return;
		}

		Map<Long, WebSocketSession> userMap = wsSessionRegistry.roomSessions.get(payload.getRoomId());

		log.info("exit -> userMap : {}", userMap);
		log.info("EXIT 전 {}번방 접속 유저 : {}", payload.getRoomId(), userMap == null ? null : userMap.keySet());
		if (userMap != null) {
			userMap.remove(myUserId);

			if (userMap.isEmpty()) {
				wsSessionRegistry.roomSessions.remove(payload.getRoomId());
			}
		}
		Map<Long, WebSocketSession> afterMap = wsSessionRegistry.roomSessions.get(payload.getRoomId()); // 확인용 Map

		log.info("EXIT 후 {}번방 접속 유저 : {}", payload.getRoomId(), afterMap == null ? null : afterMap.keySet());
		log.info("{}번 유저 {}번방 Exit 처리", myUserId, payload.getRoomId());
		wsOutboundWriter.responseOk(session, dto, "EXIT_ROOM_OK", payload);
	}//exitRoom 끝.

	//	====== 메세지 전송 ===========================================================================================================
	void handleSendMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		//		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		PayloadSendMessageDTO payload = convertPayload(dto, PayloadSendMessageDTO.class);

		log.info("{} 유저의 wsSendMsg 전송 시도! ", me.getUserId());

		if (payload.getRoomId() == null || payload.getMessageText() == null) {
			log.warn("SEND_MSG Data 누락 : {} / {}", payload.getRoomId(), payload.getMessageText());
			wsOutboundWriter.responseFail(session, dto, "MSG_SEND_FAIL", "SEND_MSG 필수값 누락");
			return;
		}

		try {

			Set<Long> viewingUserIds = wsSessionRegistry.getViewingUserIds(payload.getRoomId());

			//			ChatMessageDTO chat = chatService.sendMessage(me.getUserId(), payload, viewingUserIds);

			ChatMessageResponseDTO resChat = chatService.createMessage(me.getUserId(), me.getPublicId(), payload, viewingUserIds);

			wsOutboundWriter.broadcastToRoom(payload.getRoomId(), "MSG_CREATED", resChat, dto.getRequestId()); // chatService.sendMessage()가 성공했을 때만 broadcast해야 하니까. try{}안에 두어라.

			log.info("{}번 유저가 {}번방으로 메시지 전송: {}", me.getUserId(), payload.getRoomId(), payload.getMessageText());

		} catch (Exception e) {
			log.error("메시지 저장 실패", e);
			wsOutboundWriter.responseFail(session, dto, "MSG_SEND_FAIL", "메시지 저장 실패");
			return;
		}
		//		responseOk(session, dto, "SEND_MSG_OK", chat);
	}

	//	====== 메세지 읽기 ===========================================================================================================
	void handleReadMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		//		PayloadReadMessageDTO payload = new PayloadReadMessageDTO(); <--- @NoArgsConstructor가 없어서, 이 부분에서 터져버리는거다. 코드는 딱 1줄이라 티가나질않아서 찾기 빡셈.@NoArgsConstructor 넣고, req res 나누고 코드 쫌만 바꿨는데, 채팅 urc문제 다 해결함;;ㄷ;; 뭐야;
		//		payload.setRoomId(1L);
		//		payload.setLastReadMessageId(6L);
		//		--->
		PayloadReadMessageRequestDTO payload = convertPayload(dto, PayloadReadMessageRequestDTO.class); // ws내부의 payload를 꺼낸다.

		if (payload.getRoomId() == null || payload.getLastReadMessageId() == null) {
			log.info("readMsg 필수 값 누락 : {} / {}", payload.getRoomId(), payload.getLastReadMessageId());
			wsOutboundWriter.responseFail(session, dto, "READ_MSG_FAIL", "READ_MSG 필수값 누락");
			return;
		}

		chatService.updateLastRead(payload.getRoomId(), myUserId, payload.getLastReadMessageId()); // last_read 업뎃시킴.

		PayloadReadMessageResponseDTO responsePayload = new PayloadReadMessageResponseDTO(payload.getRoomId(), payload.getLastReadMessageId(), me.getPublicId(), me.getNickname());

		log.info("{}({})번 유저가 {}번방 {}번 메시지까지 읽음", myUserId, me.getNickname(), payload.getRoomId(), payload.getLastReadMessageId());

		wsOutboundWriter.broadcastToRoom(payload.getRoomId(), "MSG_READ", responsePayload, dto.getRequestId());
		//		responseOk(session, dto, "READ_MSG_OK", payload);
	}

	//	====== typing start/stop ===========================================================================================================
	void handleTyping(WebSocketSession session, WebSocketDTO dto, String eventType) throws Exception {
		//		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		SessionUserDTO me = wsAuth.requireLoginUser(session);

		//		PayloadTypingRequestDTO payload = convertPayload(dto, PayloadTypingRequestDTO.class);
		PayloadTypingRequestDTO payload = convertPayload(dto, PayloadTypingRequestDTO.class);

		if (payload.getRoomId() == null) {
			log.warn("TYPING Data roomId null");
			wsOutboundWriter.responseFail(session, dto, eventType + "_FAIL", "TYPING 필수값 누락");
			return;
		}

		PayloadTypingResponseDTO responsePayload = new PayloadTypingResponseDTO(payload.getRoomId(), me.getPublicId(), me.getNickname());

		log.info("{} in room={}", eventType, responsePayload.getRoomId());

		wsOutboundWriter.broadcastToRoomExceptUser(payload.getRoomId(), eventType, responsePayload, dto.getRequestId(), me.getUserId());
	}

}
