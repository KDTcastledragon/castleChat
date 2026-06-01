package com.chat.castledragon.websocket;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.castledragon.domain.ChatMessageDTO;
import com.chat.castledragon.domain.PayloadReadMessageDTO;
import com.chat.castledragon.domain.PayloadSendMessageDTO;
import com.chat.castledragon.domain.PayloadTypingDTO;
import com.chat.castledragon.domain.WebSocketDTO;
import com.chat.castledragon.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class WsChatEventHandler {

	private final ChatService chatService;

	private final ObjectMapper objectMapper = new ObjectMapper(); //JackSon 라이브러리 객체. 역할 : JSON 문자열 ↔ Java 객체 변환 === ChatHandler 내부에서 계속 재사용하는 JSON 변환기
	//	private : 이 클래스 안에서만 쓰겠다.  /  final : 한 번 만든 뒤 다른 ObjectMapper로 바꾸지 않겠다. 근데, final이라고 해서 Map 안의 내용이 못 바뀌는 건 아닙니다.Map 객체 자체는 고정이고, Map 내부 내용은 계속 변경 가능
	//	roomSessions.put(...) 또는 roomSessions.remove(...) 얘네는 가능. 하지만, roomSessions = new ConcurrentHashMap<>(); 얘는 불가능.

	public WsChatEventHandler(ChatService chatService) {
		this.chatService = chatService;

		// chatService를 생성자로 받습니다.
		// Spring이 ChatHandler를 만들 때, 이미 Bean으로 등록된 ChatService를 찾아서 넣어줍니다. 이걸 생성자 주입이라고 합니다.
		// 왜 직접 new ChatServiceImpl() 안 하냐??? Spring이 관리하는 Bean을 써야 하기 때문입니다. 트랜잭션 , AOP , 의존성 관리 , 테스트 , 싱글톤 관리 등을 쓸 수 있기 때문에.
		// 그래서,  private final ChatService chatService = new ChatServiceImpl(); <--- 이런식으로 직접 생성하지 않는다.
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

	//	====== 메세지 전송 ===========================================================================================================
	void handleSendMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		PayloadSendMessageDTO payload = convertPayload(dto, PayloadSendMessageDTO.class);

		if (payload.getRoomId() == null || payload.getSenderId() == null || payload.getMsgText() == null) {
			log.warn("SEND_MSG Data 누락 : {} / {} / {}", payload.getRoomId(), payload.getSenderId(), payload.getMsgText());
			responseFail(session, dto, "MSG_SEND_FAIL", "SEND_MSG 필수값 누락");
			return;
		}

		//		ChatDTO chat = new ChatDTO();

		try {

			Set<Long> viewingUserIds = getViewingUserIds(payload.getRoomId());

			ChatMessageDTO chat = chatService.sendMessage(payload, viewingUserIds);

			broadcastToRoom(payload.getRoomId(), "MSG_SENDED", chat, dto.getRequestId()); // chatService.sendMessage()가 성공했을 때만 broadcast해야 하니까. try{}안에 두어라.

			//			Long messageId = chatService.insertMessage(payload.getRoomId(), payload.getSenderId(), payload.getMsgText());
			//			chat.setMessageId(messageId);
			//			chat.setRoomId(payload.getRoomId());
			//			chat.setSenderId(payload.getSenderId());
			//			chat.setSenderLoginId(payload.getSenderLoginId());
			//			chat.setMsgText(payload.getMsgText());
			//			chat.setUnreadCount(null);
			//			ChatHandler는 insertMessage 직접 호출하지 않는다.
			//			ChatHandler는 ChatDTO new 하지 않는다.
			//			ChatHandler는 viewingUserIds만 구해서 Service에 넘긴다.
			//			Service가 메시지 저장 + unreadCount 계산 + ChatDTO 조립을 한다.

			log.info("{}번 유저가 {}번방으로 메시지 전송: {}", payload.getSenderId(), payload.getRoomId(), payload.getMsgText());

		} catch (Exception e) {
			log.error("메시지 저장 실패", e);
			responseFail(session, dto, "MSG_SEND_FAIL", "메시지 저장 실패");
			return;
		}
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

	//	====== typing start/stop ===========================================================================================================
	private void handleTyping(WebSocketSession session, WebSocketDTO dto, String eventType) throws Exception {
		PayloadTypingDTO payload = convertPayload(dto, PayloadTypingDTO.class);

		if (payload.getRoomId() == null || payload.getUserId() == null || payload.getLoginId() == null) {
			log.warn("TYPING Data 누락 : {} / {} / {}", payload.getRoomId(), payload.getUserId(), payload.getLoginId());
			responseFail(session, dto, eventType + "_FAIL", "TYPING 필수값 누락");
			return;
		}

		log.info("{}-({}) {} in room {}", payload.getLoginId(), payload.getUserId(), eventType, payload.getRoomId());

		broadcastToRoomExceptUser(payload.getRoomId(), eventType, payload, dto.getRequestId(), payload.getUserId());
	}

}
