package com.chat.wsgate.handler;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.domain.PayloadRoomNoticeDTO;
import com.chat.contract.domain.RoomIdRequestDTO;
import com.chat.contract.domain.SessionUserDTO;
import com.chat.contract.domain.WebSocketDTO;
import com.chat.wsgate.auth.WsAuth;
import com.chat.wsgate.domain.PayloadEnterRoomDTO;
import com.chat.wsgate.domain.PayloadExitRoomDTO;
import com.chat.wsgate.outbound.GateWayWsOutboundWriter;
import com.chat.wsgate.session.WsSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class GatewayWsRoomHandler {

	private final ObjectMapper objectMapper = new ObjectMapper(); //JackSon 라이브러리 객체. 역할 : JSON 문자열 ↔ Java 객체 변환 === ChatHandler 내부에서 계속 재사용하는 JSON 변환기
	//	private : 이 클래스 안에서만 쓰겠다.  /  final : 한 번 만든 뒤 다른 ObjectMapper로 바꾸지 않겠다. 근데, final이라고 해서 Map 안의 내용이 못 바뀌는 건 아닙니다.Map 객체 자체는 고정이고, Map 내부 내용은 계속 변경 가능
	//	roomSessions.put(...) 또는 roomSessions.remove(...) 얘네는 가능. 하지만, roomSessions = new ConcurrentHashMap<>(); 얘는 불가능.

	private final WsSessionRegistry wsSessionRegistry;
	private final GateWayWsOutboundWriter gwWsOutboundWriter;
	private final WsAuth wsAuth;
	//	private final ChatService chatService;
	//	private final ChatMetrics chatMetrics;

	// 생성자 주입
	//	public WsChatEventHandler(WsSessionRegistry wsSessionRegistry, WsOutboundWriter wsOutboundWriter, WsAuth wsAuth, ChatService chatService,
	//			ChatMetrics chatMetrics) {
	//		this.wsSessionRegistry = wsSessionRegistry;
	//		this.wsOutboundWriter = wsOutboundWriter;
	//		this.wsAuth = wsAuth;
	//		this.chatService = chatService;
	//		this.chatMetrics = chatMetrics;
	//	}

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

	//	====== 채팅방 입장 ===========================================================================================================
	public void handleEnterRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		Long myUserId = wsAuth.getMyUserIdInWsSession(session);
		PayloadEnterRoomDTO payload = convertPayload(dto, PayloadEnterRoomDTO.class);

		if (payload.getRoomId() == null || myUserId == null) {
			log.warn("ENTER_ROOM Data 누락 : {} / {}", payload.getRoomId(), myUserId);
			gwWsOutboundWriter.responseFail(session, dto, "ENTER_ROOM_FAIL", "roomId 또는 userId가 없습니다.");
			return;
		}

		//		wsSessionRegistry.roomSessions.computeIfAbsent(payload.getRoomId(), k -> new ConcurrentHashMap<>()).put(myUserId, session);
		wsSessionRegistry.enterRoomSession(payload.getRoomId(), myUserId, session);

		log.info("{}번 유저 {}번방 입장. wsSess등록.", myUserId, payload.getRoomId());
		gwWsOutboundWriter.responseOk(session, dto, "ENTER_ROOM_OK", payload);

	} // handleEnterRoom 끝.

	//	====== 채팅방 닫기 ===========================================================================================================
	public void handleExitRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		Long myUserId = wsAuth.getMyUserIdInWsSession(session);

		PayloadExitRoomDTO payload = convertPayload(dto, PayloadExitRoomDTO.class);

		if (payload.getRoomId() == null || myUserId == null) {
			gwWsOutboundWriter.responseFail(session, dto, "EXIT_ROOM_FAIL", "roomId,userId가 없습니다.");
			return;
		}

		wsSessionRegistry.exitRoomSession(payload.getRoomId(), myUserId);

		log.info("{}번 유저 {}번방 Exit 처리", myUserId, payload.getRoomId());
		gwWsOutboundWriter.responseOk(session, dto, "EXIT_ROOM_OK", payload);
	}//exitRoom 끝.

	//	====== 채팅방 나가기 ===========================================================================================================
	public void handleLeftRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsAuth.requireLoginUser(session);
		RoomIdRequestDTO payload = convertPayload(dto, RoomIdRequestDTO.class);

		if (payload.getRoomId() == null) {
			gwWsOutboundWriter.responseFail(session, dto, "LEFT_ROOM_FAIL", "roomId 없음");
			return;
		}

		//		wsSessionRegistry.exitRoomSession(payload.getRoomId(), me.getUserId()); Left 추가 필요함.

		PayloadRoomNoticeDTO notice = new PayloadRoomNoticeDTO(payload.getRoomId(), "MEMBER_LEFT", me.getNickname() + "님이 나갔습니다.", LocalDateTime.now());

		gwWsOutboundWriter.broadcastToRoom(payload.getRoomId(), "ROOM_NOTICE", notice, dto.getRequestId());
	}

}
