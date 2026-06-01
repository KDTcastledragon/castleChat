package com.chat.castledragon.websocket;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.chat.castledragon.domain.PayloadEnterRoomDTO;
import com.chat.castledragon.domain.PayloadExitRoomDTO;
import com.chat.castledragon.domain.SessionUserDTO;
import com.chat.castledragon.domain.WebSocketDTO;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class WsSessionRegistry {

	private final Map<Long, Map<Long, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
	// final : 한 번 만든 뒤 다른 ObjectMapper로 바꾸지 않겠다. 근데, final이라고 해서 Map 안의 내용이 못 바뀌는 건 아닙니다.Map 객체 자체는 고정이고, Map 내부 내용은 계속 변경 가능
	// final : 이 클래스가 생성될 때 주입받은 WsSessionRegistry 참조를 나중에 다른 객체로 바꿔치기하지 못하게 막는 것.
	// -> 여러 클래스가 각자 Map을 만들지 않고 Spring이 만든 WsSessionRegistry 하나를 공유해서하나의 접속 명부를 공통 관리하게 한다
	// -->> 정확히 말하자면 : final이 “공유”를 만들어주는 건 아니야.공유를 만들어주는 건 Spring singleton Bean 주입이고, final은 그 주입받은 참조를 바꾸지 못하게 '고정'하는 역할이야.
	// roomSessions.put(...) 또는 roomSessions.remove(...) 얘네는 가능. 하지만, roomSessions = new ConcurrentHashMap<>(); 얘는 불가능.

	// 채팅방별로 현재 접속 중인 유저들의 WebSocket 연결을 저장할 명부를 하나 준비한다.
	//	private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();// 근데 왜 List안쓰고 Set을 씀? (Legacy:OnlyRoom)
	// roomId별로 현재 접속 중인 WebSocketSession들을 저장.
	//	왜 필요하냐??? 방에 메시지가 왔을 때, 이 roomId에 접속 중인 사람들에게만 보내야 하니까.
	//	 <구조>
	//	roomSessions
	//	└─ roomId
	//	   └─ userId
	//	      └─ WebSocketSession

	private final Map<WebSocketSession, SessionUserDTO> connectedUserSessions = new ConcurrentHashMap<>();
	//	ConcurrentHashMap 왜 씀?? 일반 HashMap은 여러 thread가 동시에 수정하면 문제가 생길 수 있습니다.그래서 thread-safe한 Map인 ConcurrentHashMap씀. 동시에 여러 요청이 건드려도 일반 HashMap보다 안전한 Map
	//	private final Map<WebSocketSession, PayloadConnectUserDTO> connectedUserSessions = new ConcurrentHashMap<>(); // HashMap은 thread-safe하지 않아서 꼬일 수 있어.

	//	====== 유저 접속 ============================================================================================================
	private void handleConnectUser(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO loginUser = getLoginUser(session);

		if (loginUser == null) {
			log.warn("인증되지 않은 WS CONNECT 요청. WSid={}", session.getId());
			responseFail(session, dto, "CONNECT_USER_FAIL", "로그인이 필요합니다.");

			if (session.isOpen()) {
				session.close(CloseStatus.NOT_ACCEPTABLE);
			}

			return;
		}

		connectedUserSessions.put(session, loginUser);

		log.info("{}-({})님이 접속하셨습니다.", loginUser.getNickname(), loginUser.getUserId());
		responseOk(session, dto, "CONNECT_USER_OK", loginUser);
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

	}

	//	====== 채팅방 입장 ===========================================================================================================
	void handleEnterRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		PayloadEnterRoomDTO payload = convertPayload(dto, PayloadEnterRoomDTO.class);

		if (payload.getRoomId() == null || payload.getUserId() == null) {
			log.warn("ENTER_ROOM Data 누락 : {} / {}", payload.getRoomId(), payload.getUserId());
			responseFail(session, dto, "ENTER_ROOM_FAIL", "roomId 또는 userId가 없습니다.");
			return;
		}

		roomSessions.computeIfAbsent(payload.getRoomId(), k -> new ConcurrentHashMap<>()).put(payload.getUserId(), session);
		log.info("{}번 유저 {}번방 ENTER 등록", payload.getUserId(), payload.getRoomId());
		responseOk(session, dto, "ENTER_ROOM_OK", payload);

	} // handleEnterRoom 끝.

	//	====== 채팅방 닫기 ===========================================================================================================
	void handleExitRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		PayloadExitRoomDTO payload = convertPayload(dto, PayloadExitRoomDTO.class);

		if (payload.getRoomId() == null || payload.getUserId() == null) {
			responseFail(session, dto, "EXIT_ROOM_FAIL", "roomId 또는 userId가 없습니다.");
			return;
		}

		Map<Long, WebSocketSession> userMap = roomSessions.get(payload.getRoomId());

		log.info("exit -> userMap : {}", userMap);
		log.info("EXIT 전 {}번방 접속 유저 : {}", payload.getRoomId(), userMap == null ? null : userMap.keySet());
		if (userMap != null) {
			userMap.remove(payload.getUserId());

			if (userMap.isEmpty()) {
				roomSessions.remove(payload.getRoomId());
			}
		}
		Map<Long, WebSocketSession> afterMap = roomSessions.get(payload.getRoomId()); // 확인용 Map

		log.info("EXIT 후 {}번방 접속 유저 : {}", payload.getRoomId(), afterMap == null ? null : afterMap.keySet());
		log.info("{}번 유저 {}번방 Exit 처리", payload.getUserId(), payload.getRoomId());
		responseOk(session, dto, "EXIT_ROOM_OK", payload);
	}//exitRoom 끝.

	// ====== 모든 방 exit 처리 ===========================================================================================================
	void removeSessionAllRooms(WebSocketSession session) {
		roomSessions.forEach((roomId, userMap) -> {
			userMap.entrySet().removeIf(entry -> entry.getValue().equals(session));

			if (userMap.isEmpty()) {
				roomSessions.remove(roomId);
			}
		});
	}

	// ====== 현재 채팅방 접속중인 유저찾기 ===========================================================================================================
	Set<Long> getViewingUserIds(Long roomId) {
		Map<Long, WebSocketSession> sessions = roomSessions.get(roomId);

		if (sessions == null || sessions.isEmpty()) {
			return Set.of();
		}

		sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());

		return new HashSet<>(sessions.keySet());
	}

	//	로그인 인증 =======================================================
	private SessionUserDTO getLoginUser(WebSocketSession session) {
		Object loginUser = session.getAttributes().get("LOGIN_USER");

		if (loginUser instanceof SessionUserDTO user) {
			return user;
		}

		return null;
	}

	private Long getLoginUserId(WebSocketSession session) {
		SessionUserDTO loginUser = getLoginUser(session);

		if (loginUser == null) {
			return null;
		}

		return Long.valueOf(loginUser.getUserId());
	}

}// WsSessionRegistry
