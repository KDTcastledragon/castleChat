package com.chat.castledragon.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.chat.castledragon.domain.ChatDTO;
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

	//	====== 메세지 관리 : switch-case ===========================================================================================================
	// TextWebSocketHandler안에 handleTextMessage내장 메소드 존재. 그래서 @Override 붙임. 반드시 약속된 메서드인 handleTextMessage를 입구로 써야 합니다.
	@Override // --> 부모 클래스에 이미 있는 메서드를 내가 원하는 방식으로 다시 작성한다.
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

		log.info("webSocket_msg 도착 : {}", message.getPayload());

		ChatDTO dto = objectMapper.readValue(message.getPayload(), ChatDTO.class); // JSON 문자열을 ChatDTO 객체로 바꿔라

		switch (dto.getType()) {
		case "ENTER_ROOM" -> handleEnterRoom(session, dto);
		case "SEND_MSG" -> handleSendMessage(session, dto);
		case "READ_MSG" -> handleReadMessage(session, dto);
		default -> log.warn("알 수 없는 TYPE WS : {}", dto.getType());
		}

		// 확장성 , 책임분리 ,버그 전파 방지 위해 분리했따.   1. 메시지를 받는다 --> 2. type에 맞는 method로 보낸다.  dispatcher의 역할을 한다.
		//		처음에는 handleTextMessage 안에 모든 로직을 넣을 수도 있지만, WebSocket 메시지 타입이 늘어나면 메서드가 비대해지고 각 이벤트의 필수값 검증과 예외 처리가 섞이게 됩니다. 
		//		그래서 handleTextMessage는 type 기반 dispatcher 역할만 하도록 두고, 실제 처리는 handleEnterRoom, handleSendMessage, handleReadMessage로 분리했습니다. 
		//		이렇게 하면 각 이벤트의 책임이 분명해지고, read/unread 문제처럼 특정 기능을 수정할 때 해당 메서드만 보면 되어 유지보수성과 테스트 용이성이 좋아집니다.

		//		WebSocket은 HTTP처럼 요청과 응답이 1:1로 묶여 있지 않고, 하나의 연결에서 여러 요청, 응답, broadcast 이벤트가 섞여 들어옵니다. 
		//		그래서 클라이언트가 보낸 특정 요청과 서버의 응답을 매칭하기 위해 requestId를 추가했습니다. 클라이언트는 메시지를 보낼 때 UUID를 생성해 함께 보내고, 
		//		서버는 처리 결과를 같은 requestId로 ACK 또는 FAIL 이벤트에 담아 반환합니다. 
		//		이를 통해 메시지 전송 성공/실패 처리, 낙관적 UI의 임시 메시지 교체, timeout 처리 등을 안정적으로 구현할 수 있습니다.

		//		처음에는 ChatDTO 하나로 WebSocket 요청과 메시지 데이터를 함께 처리했지만, 이벤트 타입이 늘어나면서 DTO의 책임이 섞인다고 판단했습니다. 
		//		그래서 WebSocket 요청/응답 envelope를 도입해 requestId, type, success, errorMessage 같은 공통 메타데이터는 envelope에 두고, 
		//		실제 이벤트별 데이터는 payload DTO로 분리했습니다. 
		//		이 구조는 요청-응답 매칭과 에러 처리, broadcast 이벤트 처리를 일관되게 만들 수 있다는 장점이 있습니다.

	} // handleTextMessage 끝.

	//	====== room 퇴장 감지 ===========================================================================================================
	private void handleEnterRoom(WebSocketSession session, ChatDTO dto) {
		if (dto.getRoomId() == null || dto.getUserId() == null) {
			log.warn("ENTER_ROOM Data 누락 : {} / {}", dto.getRoomId(), dto.getUserId());
			return;
		}
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
	}
}

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