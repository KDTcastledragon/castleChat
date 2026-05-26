package com.chat.castledragon.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.chat.castledragon.domain.ChatDTO;
import com.chat.castledragon.domain.PayloadEnterRoomDTO;
import com.chat.castledragon.domain.PayloadReadMessageDTO;
import com.chat.castledragon.domain.PayloadSendMessageDTO;
import com.chat.castledragon.domain.WebSocketDTO;
import com.chat.castledragon.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class ChatHandler extends TextWebSocketHandler {

	private final ChatService chatService;
	private final ObjectMapper objectMapper = new ObjectMapper(); //JackSon 라이브러리 객체. 역할 : JSON 문자열 ↔ Java 객체 변환 === ChatHandler 내부에서 계속 재사용하는 JSON 변환기
	//	private : 이 클래스 안에서만 쓰겠다.  /  final : 한 번 만든 뒤 다른 ObjectMapper로 바꾸지 않겠다. 근데, final이라고 해서 Map 안의 내용이 못 바뀌는 건 아닙니다.Map 객체 자체는 고정이고, Map 내부 내용은 계속 변경 가능
	//	roomSessions.put(...) 또는 roomSessions.remove(...) 얘네는 가능. 하지만, roomSessions = new ConcurrentHashMap<>(); 얘는 불가능.

	// 채팅방별로 현재 접속 중인 유저들의 WebSocket 연결을 저장할 명부를 하나 준비한다.
	//	private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();// 근데 왜 List안쓰고 Set을 씀? (Legacy:OnlyRoom)
	private final Map<Long, Map<Long, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
	// roomId별로 현재 접속 중인 WebSocketSession들을 저장.
	//	왜 필요하냐??? 방에 메시지가 왔을 때, 이 roomId에 접속 중인 사람들에게만 보내야 하니까.
	//	 <구조>
	//	roomSessions
	//	└─ roomId
	//	   └─ userId
	//	      └─ WebSocketSession

	//	ConcurrentHashMap 왜 씀?? 일반 HashMap은 여러 thread가 동시에 수정하면 문제가 생길 수 있습니다.그래서 thread-safe한 Map인 ConcurrentHashMap씀. 동시에 여러 요청이 건드려도 일반 HashMap보다 안전한 Map

	//	========== 생성자 주입 ==========================================================================================
	//	메시지 저장/읽음 처리 같은 비즈니스 로직은 Service에게 맡기기 위해 Spring이 넣어준 ChatService를 보관한다.
	public ChatHandler(ChatService chatService) {
		this.chatService = chatService;

		// chatService를 생성자로 받습니다.
		// Spring이 ChatHandler를 만들 때, 이미 Bean으로 등록된 ChatService를 찾아서 넣어줍니다. 이걸 생성자 주입이라고 합니다.
		// 왜 직접 new ChatServiceImpl() 안 하냐??? Spring이 관리하는 Bean을 써야 하기 때문입니다. 트랜잭션 , AOP , 의존성 관리 , 테스트 , 싱글톤 관리 등을 쓸 수 있기 때문에.
		// 그래서,  private final ChatService chatService = new ChatServiceImpl(); <--- 이런식으로 직접 생성하지 않는다.
	}

	//	====== 연결 이후 메소드 ===========================================================================================================
	@Override
	public void afterConnectionEstablished(WebSocketSession session) { // 이게 실행되는 순간 = 클라이언트가 ws 연결 성공한 순간
		log.info("WebSocket 연결 성공 : {}", session);
	}

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

	//	====== 메세지 관리 Dispatcher ===========================================================================================================
	// TextWebSocketHandler안에 handleTextMessage내장 메소드 존재. 그래서 @Override 붙임. 반드시 약속된 메서드인 handleTextMessage를 입구로 써야 합니다.
	@Override // --> 부모 클래스에 이미 있는 메서드를 내가 원하는 방식으로 다시 작성한다.
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

		log.info("webSocket_msg 도착 : {}", message.getPayload());

		//		ChatDTO dto = objectMapper.readValue(message.getPayload(), ChatDTO.class); // JSON 문자열을 ChatDTO 객체로 바꿔라 --> WebSocketDTO로 변경되어 legacy.
		WebSocketDTO dto = objectMapper.readValue(message.getPayload(), WebSocketDTO.class);

		if (dto.getWsType() == null) {
			log.warn("WebSocket type 없음: {}", message.getPayload());
			return;
		}

		switch (dto.getWsType()) {
		case "ENTER_ROOM" -> handleEnterRoom(session, dto);
		case "SEND_MSG" -> handleSendMessage(session, dto);
		case "READ_MSG" -> handleReadMessage(session, dto);
		default -> {
			log.warn("알 수 없는 WS TYPE : {}", dto.getWsType());
			responseFail(session, dto, "UNKNOWN_TYPE", "알 수 없는 WS TYPE");
		}

		}

	} // handleTextMessage 끝.

	//	====== 채팅방 입장 ===========================================================================================================
	private void handleEnterRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		PayloadEnterRoomDTO payload = convertPayload(dto, PayloadEnterRoomDTO.class);

		if (payload.getRoomId() == null || payload.getUserId() == null) {
			log.warn("ENTER_ROOM Data 누락 : {} / {}", payload.getRoomId(), payload.getUserId());
			responseFail(session, dto, "ENTER_ROOM_FAIL", "roomId 또는 userId가 없습니다.");
			return;
		}

		// payload.roomId 방의 명부가 없으면 새로 만들고, 그 방 명부에 payload.userId와 현재 WebSocketSession을 등록한다. -->
		//		
		//		Long roomId = payload.getRoomId();
		//		Long userId = payload.getUserId();
		//
		//		Map<Long, WebSocketSession> userMap = roomSessions.get(roomId);
		//
		//		if (userMap == null) {
		//		    userMap = new ConcurrentHashMap<>();
		//		    roomSessions.put(roomId, userMap);
		//		}
		//
		//		userMap.put(userId, session);

		// --> 이 유저의 현재 WebSocket 연결은 이 roomId의 실시간 메시지를 받을 대상이다.

		//	위 과정을 축약했다. -->
		roomSessions.computeIfAbsent(payload.getRoomId(), k -> new ConcurrentHashMap<>()).put(payload.getUserId(), session);
		log.info("{}번 유저 {}번방 ENTER 등록", payload.getUserId(), payload.getRoomId());
		responseOk(session, dto, "ENTER_ROOM_OK", payload);

	} // handleEnterRoom 끝.

	//	====== 메세지 읽기 ===========================================================================================================
	private void handleSendMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		PayloadSendMessageDTO payload = convertPayload(dto, PayloadSendMessageDTO.class);

		if (payload.getRoomId() == null || payload.getSenderId() == null || payload.getMsgText() == null) {
			log.warn("SEND_MSG Data 누락 : {} / {} / {}", payload.getRoomId(), payload.getSenderId(), payload.getMsgText());
			responseFail(session, dto, "MSG_SEND_FAIL", "SEND_MSG 필수값 누락");
			return;
		}

		ChatDTO chat = new ChatDTO();

		try {
			Long messageId = chatService.insertMessage(payload.getRoomId(), payload.getSenderId(), payload.getMsgText());

			chat.setMessageId(messageId);
			chat.setRoomId(payload.getRoomId());
			chat.setSenderId(payload.getSenderId());
			chat.setSenderLoginId(payload.getSenderLoginId());
			chat.setMsgText(payload.getMsgText());

			log.info("{}번 유저가 {}번방으로 메시지 전송: {}", payload.getSenderId(), payload.getRoomId(), payload.getMsgText());

		} catch (Exception e) {
			log.error("메시지 저장 실패", e);
			responseFail(session, dto, "MSG_SEND_FAIL", "메시지 저장 실패");
			return;
		}

		broadcastToRoom(payload.getRoomId(), "MSG_SENDED", chat, dto.getRequestId());
		//		responseOk(session, dto, "SEND_MSG_OK", chat);
	}

	//	====== 메세지 읽기 ===========================================================================================================
	private void handleReadMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		PayloadReadMessageDTO payload = convertPayload(dto, PayloadReadMessageDTO.class);

		if (payload.getRoomId() == null || payload.getUserId() == null || payload.getLastReadMessageId() == null) {
			log.info("readMsg 필수 값 누락 : {} / {} / {}", payload.getRoomId(), payload.getUserId(), payload.getLastReadMessageId());
			responseFail(session, dto, "READ_MSG_FAIL", "READ_MSG 필수값 누락");
			return;
		}

		chatService.updateLastRead(payload.getRoomId(), payload.getUserId(), payload.getLastReadMessageId());

		log.info("{}번 유저가 {}번방 {}번 메시지까지 읽음", payload.getUserId(), payload.getRoomId(), payload.getLastReadMessageId());

		broadcastToRoom(payload.getRoomId(), "MSG_READ", payload, dto.getRequestId());
		//		responseOk(session, dto, "READ_MSG_OK", payload);
	}

	//	====== broadcast ===========================================================================================================
	private void broadcastToRoom(Long roomId, String type, Object payloadData, String requestId) throws Exception {
		Map<Long, WebSocketSession> sessions = roomSessions.get(roomId);

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

	//	====== Success 응답 보내기 ===========================================================================================================
	private void responseOk(WebSocketSession session, WebSocketDTO request, String wsType, Object payload) throws Exception {
		WebSocketDTO response = new WebSocketDTO();

		response.setRequestId(request.getRequestId());
		response.setWsType(wsType);
		response.setIsSuccess(true);
		response.setPayload(objectMapper.valueToTree(payload));

		dispatchToSession(session, response);
	}

	//	====== Fail 응답 보내기 ===========================================================================================================
	private void responseFail(WebSocketSession session, WebSocketDTO request, String wsType, String errorMessage) throws Exception {
		WebSocketDTO response = new WebSocketDTO();

		response.setRequestId(request.getRequestId());
		response.setWsType(wsType);
		response.setIsSuccess(false);
		response.setWsMessage(errorMessage);

		dispatchToSession(session, response);
	}

	//	====== 응답 Session에 보내기 ===========================================================================================================
	private void dispatchToSession(WebSocketSession session, WebSocketDTO dto) throws Exception {
		if (!session.isOpen()) {
			log.info("responseToSession is Not Open");
			return;
		}

		String payload = objectMapper.writeValueAsString(dto);
		session.sendMessage(new TextMessage(payload));
	}

	//	====== room 퇴장 감지 ===========================================================================================================
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

		roomSessions.forEach((roomId, userMap) -> {

			userMap.entrySet().removeIf(entry -> entry.getValue().equals(session));

			// 방이 비었으면 room 자체 제거
			if (userMap.isEmpty()) {
				roomSessions.remove(roomId);
			}
		});

		log.info("WebSocket 연결 종료");
	} // afterConnectionClosed 끝.

} // ChatHandler 끝.

//	확장성 , 책임분리 ,버그 전파 방지 위해 분리했따.   1. 메시지를 받는다 --> 2. type에 맞는 method로 보낸다.  dispatcher의 역할을 한다.
//		처음에는 handleTextMessage 안에 모든 로직을 넣을 수도 있지만, WebSocket 메시지 타입이 늘어나면 메서드가 비대해지고 각 이벤트의 필수값 검증과 예외 처리가 섞이게 됩니다. 
//		그래서 handleTextMessage는 type 기반 dispatcher 역할만 하도록 두고, 실제 처리는 handleEnterRoom, handleSendMessage, handleReadMessage로 분리했습니다. 
//		이렇게 하면 각 이벤트의 책임이 분명해지고, read/unread 문제처럼 특정 기능을 수정할 때 해당 메서드만 보면 되어 유지보수성과 테스트 용이성이 좋아집니다.

//		WebSocket은 HTTP처럼 요청과 응답이 1:1로 묶여 있지 않고, 하나의 연결에서 여러 요청, 응답, broadcast 이벤트가 섞여 들어옵니다. 
//		그래서 클라이언트가 보낸 특정 요청과 서버의 응답을 매칭하기 위해 requestId를 추가했습니다. 클라이언트는 메시지를 보낼 때 UUID를 생성해 함께 보내고, 
//		서버는 처리 결과를 같은 requestId로 ACK 또는 FAIL 이벤트에 담아 반환합니다. 
//		이를 통해 메시지 전송 성공/실패 처리, 낙관적 UI의 임시 메시지 교체, timeout 처리 등을 안정적으로 구현할 수 있습니다.

//		처음에는 ChatDTO 하나로 WebSocket 요청과 메시지 데이터를 함께 처리했지만, 이벤트 타입이 늘어나면서 DTO의 책임이 섞인다고 판단했습니다. 
//		그래서 ChatDTO와 WebSocketDTO로 분리하여 만들었는데, 
//		어떤 type에서 어떤 필드가 필수인지 알기 어려움 , null 필드가 엄청 많아짐 , 검증이 지저분해짐 , 프론트와 계약이 흐려짐 , DTO 하나가 모든 일을 다 함 등의 문제가 발생.
//		그래서 WebSocket 요청/응답 envelope를 도입해 requestId, type, success, errorMessage 같은 공통 메타데이터는 envelope에 두고, 
//		실제 이벤트별 데이터는 payload DTO로 분리했습니다. 
//		이 구조는 요청-응답 매칭과 에러 처리, broadcast 이벤트 처리를 일관되게 만들 수 있다는 장점이 있습니다.

//		Envelope를 도입한 목적은 단순히 WebSocket 응답을 보내기 위해서가 아니라, WebSocket 메시지의 공통 프로토콜을 정의하기 위해서였습니다. 
//		WebSocket은 HTTP처럼 endpoint와 응답이 분리되어 있지 않고 모든 이벤트가 하나의 onmessage로 들어오기 때문에, 
//		type, requestId, success, errorMessage, payload를 가진 공통 envelope를 사용해 요청, 응답, broadcast 이벤트를 일관되게 처리하도록 설계했습니다. 
//		이를 통해 이벤트 라우팅, 요청-응답 매칭, 에러 처리, 기능 확장을 더 명확하게 만들 수 있었습니다.
//		envelope는 “응답 하나 보내려고 억지로 만드는 구조”가 아니라, 앞으로 WebSocket 프로토콜을 감당하기 위한 기초 설계가 됩니다.

//		네이밍은 언어 차원의 규칙보다는 팀 컨벤션의 문제라고 생각합니다. 
//		저는 이벤트 이름을 먼저 두고 역할을 뒤에 붙이는 방식이 SendMessagePayloadDTO, SendMessageResponseDTO처럼 관련 클래스를 도메인 기준으로 찾기 쉬워서 선호합니다. 
//		다만 Payload DTO를 한 그룹으로 모으는 컨벤션을 선택한다면 PayloadSendMessageDTO처럼 앞에 붙이는 것도 가능하다고 봅니다.

//		메시지 전송과 읽음 처리는 성공 시 별도의 ACK를 보내지 않고, 저장/처리 성공 후 발생하는 domain event인 MSG_CREATED, MSG_READ broadcast를 성공 응답으로 사용했습니다. 
//		WebSocket에서는 송신자도 같은 room을 구독하고 있기 때문에 broadcast 이벤트를 받을 수 있고, 해당 이벤트에 requestId를 포함시켜 낙관적 UI의 임시 메시지와 매칭할 수 있게 했습니다. 
//		실패한 경우에만 요청자에게 SEND_MSG_FAIL, READ_MSG_FAIL을 전송해 중복 응답을 줄였습니다.

//	@Override
//	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//
//		log.info("메시지 도착 : {}", message);
//
//		// 1. JSON → DTO
//		ChatDTO dto = objectMapper.readValue(message.getPayload(), ChatDTO.class);
//
//		Long roomId = dto.getRoomId();
//
//		roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
//
//		// 2. DB 저장
//		try {
//			chatService.insertMessage(dto.getRoomId(), dto.getSenderId(), dto.getMsgText());
//
//			log.info("{}의 메세지가 {}방으로 도착 : {}", dto.getSenderId(), dto.getRoomId(), dto.getMsgText());
//
//			log.info("DB 저장 완료");
//			log.info("■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■");
//		} catch (Exception e) {
//
//			log.info("메시지 도달했으나, 저장은 실패 : {} / {}", dto.getMsgText(), e); // ws연결 유지를 위해 try-catch 도입.
//			log.error("메시지 저장 실패 : {}", e); // ws연결 유지를 위해 try-catch 도입.
//		}
//
//		// 3. 브로드캐스트용 메시지 생성
//		String payload = objectMapper.writeValueAsString(dto);
//
//		// 4. 같은 room 사람들에게 전송
//		Set<WebSocketSession> sessions = roomSessions.get(roomId); // 근데 왜 List안쓰고 Set을 씀?같은 session 중복 저장됨/disconnect 처리 어려움/브로드캐스트 중복 발생/그래서 WebSocket은 거의 무조건 Set 쓴다
//
//		if (sessions != null) {
//
//			sessions.removeIf(socketSession -> !socketSession.isOpen());
//
//			for (WebSocketSession s : sessions) {
//				try {
//					if (s.isOpen()) {
//						s.sendMessage(new TextMessage(payload));
//					}
//				} catch (Exception e) {
//					log.error("전송 실패 session 제거", e);
//					sessions.remove(s);
//				}
//			}
//		}
//	}
//
//	@Override
//	public void afterConnectionEstablished(WebSocketSession session) {
//
//		String query = session.getUri().getQuery();
//
//		if (query == null || !query.contains("roomId=")) {
//			log.error("roomId 없음 - 연결 종료");
//			return;
//		}
//
//		Long roomId = Long.valueOf(query.split("=")[1]);
//
//		roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
//	}

// ========================== query string --> enter 이벤트 처리로 변경해서 legacy로 남김.====================
//@Override 
//public void afterConnectionEstablished(WebSocketSession session) { // 이게 실행되는 순간 = 클라이언트가 ws 연결 성공한 순간
//	// ws://localhost:8080/ws/chat?roomId=3&userId=7의 ?뒤의 부분 --> roomId=3&userId=7 을 'query string' 이라고 부른다.
//	// Get대신 Post를 못 쓰는 이유 --> 브라우저의 WebSocket API가 body를 지원하지 않기 때문입니다.
//
//	String query = session.getUri().getQuery();
//	//session은 현재 연결된 WebSocket 연결 정보입니다. session.getUri()는 클라이언트가 접속한 주소를 가져옵니다. .getQuery()는 ? 뒤쪽만 꺼냅니다.
//
//	if (query == null) {
//		log.error("query 없음");
//		return; // 여기서 return하면 연결 자체를 강제로 닫는 건 아니지만, 이 session은 roomSessions에 등록되지 않습니다. 추후 broadcast 대상에도 포함되지 않습니다.
//	}
//
//	String[] params = query.split("&");
//
//	Long roomId = null;
//	Long userId = null;
//
//	for (String param : params) {
//
//		String[] keyValue = param.split("=");
//
//		if (keyValue[0].equals("roomId")) {
//			roomId = Long.valueOf(keyValue[1]);
//		}
//
//		if (keyValue[0].equals("userId")) {
//			userId = Long.valueOf(keyValue[1]);
//		}
//	}
//
//	if (roomId == null || userId == null) {
//
//		log.error("roomId 또는 userId 없음");
//
//		return;
//	}
//
//	roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
//
//	log.info("{}번 유저 {}번방 입장", userId, roomId);
//}

//
//@Override
//public void afterConnectionEstablished__legacy(WebSocketSession session) { // 이게 실행되는 순간 = 클라이언트가 ws 연결 성공한 순간
//	// ws://localhost:8080/ws/chat?roomId=3&userId=7의 ?뒤의 부분 --> roomId=3&userId=7 을 'query string' 이라고 부른다.
//	// Get대신 Post를 못 쓰는 이유 --> 브라우저의 WebSocket API가 body를 지원하지 않기 때문입니다.
//
//	String query = session.getUri().getQuery();
//	//session은 현재 연결된 WebSocket 연결 정보입니다. session.getUri()는 클라이언트가 접속한 주소를 가져옵니다. .getQuery()는 ? 뒤쪽만 꺼냅니다.
//
//	if (query == null) {
//		log.error("query 없음");
//		return; // 여기서 return하면 연결 자체를 강제로 닫는 건 아니지만, 이 session은 roomSessions에 등록되지 않습니다. 추후 broadcast 대상에도 포함되지 않습니다.
//	}
//
//	String[] params = query.split("&");
//
//	Long roomId = null;
//	Long userId = null;
//
//	for (String param : params) {
//
//		String[] keyValue = param.split("=");
//
//		if (keyValue[0].equals("roomId")) {
//			roomId = Long.valueOf(keyValue[1]);
//		}
//
//		if (keyValue[0].equals("userId")) {
//			userId = Long.valueOf(keyValue[1]);
//		}
//	}
//
//	if (roomId == null || userId == null) {
//
//		log.error("roomId 또는 userId 없음");
//
//		return;
//	}
//
//	roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
//
//	log.info("{}번 유저 {}번방 입장", userId, roomId);
//}

// ========실무 스타일로 리팩토링. LEGACY_CODE=====================================================================
//@Override // --> 부모 클래스에 이미 있는 메서드를 내가 원하는 방식으로 다시 작성한다.
//protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//
//	log.info("메시지 도착 : {}", message);
//
//	// 1. JSON → DTO
//	ChatDTO dto = objectMapper.readValue(message.getPayload(), ChatDTO.class); // JSON 문자열을 ChatDTO 객체로 바꿔라
//
//	Long roomId = dto.getRoomId();
//
//	if ("ENTER_ROOM".equals(dto.getType())) {
//		roomSessions.computeIfAbsent(dto.getRoomId(), k -> new ConcurrentHashMap<>()).put(dto.getUserId(), session);
//
//		log.info("{}번 유저 {}번방 ENTER 등록", dto.getUserId(), dto.getRoomId());
//		return;
//	}
//
//	if ("READ_MSG".equals(dto.getType())) {
//		chatService.updateLastRead(dto.getRoomId(), dto.getUserId(), dto.getLastReadMessageId());
//		log.info("{}의 {}방 {}번 메시지까지 읽음", dto.getUserId(), dto.getRoomId(), dto.getLastReadMessageId());
//		return;
//	}
//
//	// 2. DB 저장
//	try {
//
//		Long messageId = chatService.insertMessage(dto.getRoomId(), dto.getSenderId(), dto.getMsgText());
//		dto.setMessageId(messageId);
//
//		log.info("{}의 메세지가 {}방으로 도착 : {}", dto.getSenderId(), dto.getRoomId(), dto.getMsgText());
//
//		log.info("DB 저장 완료");
//		log.info("■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■");
//
//	} catch (Exception e) {
//
//		log.info("메시지 도달했으나, 저장은 실패 : {} / {}", dto.getMsgText(), e);
//
//		log.error("메시지 저장 실패 : {}", e);
//	}
//
//	// 3. 브로드캐스트용 메시지 생성
//	String payload = objectMapper.writeValueAsString(dto);  // ChatDTO 객체를 JSON 문자열로 바꿔라
//
//	// 4. 같은 room 사람들에게 전송
//	Map<Long, WebSocketSession> sessions = roomSessions.get(roomId);
//
//	if (sessions != null) {
//
//		// 닫힌 세션 제거
//		sessions.values().removeIf(socketSession -> !socketSession.isOpen());
//
//		for (WebSocketSession s : sessions.values()) {
//
//			try {
//
//				if (s.isOpen()) {
//					s.sendMessage(new TextMessage(payload));
//				}
//
//			} catch (Exception e) {
//
//				log.error("전송 실패 session 제거", e);
//
//				// sessions.values().remove(s);  // 반복문 도는 중 remove는 위험하다. ConcurrentHashMap라 덜 위험하긴 한데, 굳이 안 하는 게 좋음. sessions.values().removeIf(socketSession -> !socketSession.isOpen()); 하고있어서 ㄱㅊ.
//			}
//		}
//	}
//}