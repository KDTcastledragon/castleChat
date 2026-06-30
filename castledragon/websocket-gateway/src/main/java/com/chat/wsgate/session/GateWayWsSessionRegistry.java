package com.chat.wsgate.session;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.domain.SessionUserDTO;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class GateWayWsSessionRegistry {
	private final Map<WebSocketSession, SessionUserDTO> wsSessionsToConnectedUser = new ConcurrentHashMap<>();
	private final Map<Long, WebSocketSession> userIdToWsSessions = new ConcurrentHashMap<>(); // userId:ws 1:1대응이라서 단수형으로 naming.

	private final Map<Long, Map<Long, WebSocketSession>> roomSessions = new ConcurrentHashMap<>(); // room하나에 여러 sessions들어가서 복수형으로 naming.
	// final : 한 번 만든 뒤 다른 ObjectMapper로 바꾸지 않겠다. 근데, final이라고 해서 Map 안의 내용이 못 바뀌는 건 아닙니다.Map 객체 자체는 고정이고, Map 내부 내용은 계속 변경 가능
	// final : 이 클래스가 생성될 때 주입받은 WsSessionRegistry 참조를 나중에 다른 객체로 바꿔치기하지 못하게 막는 것.
	// -> 여러 클래스가 각자 Map을 만들지 않고 Spring이 만든 WsSessionRegistry 하나를 공유해서하나의 접속 명부를 공통 관리하게 한다
	// -->> 정확히 말하자면 : final이 “공유”를 만들어주는 건 아니야.공유를 만들어주는 건 Spring singleton Bean 주입이고, final은 그 주입받은 참조를 바꾸지 못하게 '고정'하는 역할이야.
	// roomSessions.put(...) 또는 roomSessions.remove(...) 얘네는 가능. 하지만, roomSessions = new ConcurrentHashMap<>(); 얘는 불가능.
	// private + final : 세션 명부의 원본은 하나만 둔다. 그 원본은 아무나 직접 못 만진다. 오직 SessionRegi 내부에 정해진 메서드로만 '간접적으로' Map<>내부의 데이터를 접근/변경한다.

	// 채팅방별로 현재 접속 중인 유저들의 WebSocket 연결을 저장할 명부를 하나 준비한다.
	//	private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();// 근데 왜 List안쓰고 Set을 씀? (Legacy:OnlyRoom)
	// roomId별로 현재 접속 중인 WebSocketSession들을 저장.
	//	왜 필요하냐??? 방에 메시지가 왔을 때, 이 roomId에 접속 중인 사람들에게만 보내야 하니까.
	//	 <구조>
	//	roomSessions
	//	└─ roomId
	//	   └─ userId
	//	      └─ WebSocketSession

	//	ConcurrentHashMap 왜 씀?? 일반 HashMap은 여러 thread가 동시에 수정하면 문제가 생길 수 있습니다.그래서 thread-safe한 Map인 ConcurrentHashMap씀. 동시에 여러 요청이 건드려도 일반 HashMap보다 안전한 Map
	//	private final Map<WebSocketSession, PayloadConnectUserDTO> connectedUserSessions = new ConcurrentHashMap<>(); // HashMap은 thread-safe하지 않아서 꼬일 수 있어.

	// ====== 로그인 유저 등록 ===========================================================================================================
	public void registerConnectedUser(WebSocketSession session, SessionUserDTO loginUser) {
		wsSessionsToConnectedUser.put(session, loginUser);
		userIdToWsSessions.put(loginUser.getUserId(), session);
	}

	// ====== 로그아웃 세션 삭제 ===========================================================================================================
	public SessionUserDTO removeConnectedUser(WebSocketSession session) {
		SessionUserDTO removedUser = wsSessionsToConnectedUser.remove(session);

		if (removedUser != null) {
			userIdToWsSessions.remove(removedUser.getUserId(), session);
		}

		return removedUser;
	}

	// ====== roomSession에 등록 ===========================================================================================================
	public void enterRoomSession(Long roomId, Long userId, WebSocketSession session) {
		roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
	}

	// ====== roomSession에서 삭제 ===========================================================================================================
	public void exitRoomSession(Long roomId, Long userId) {
		Map<Long, WebSocketSession> enteredUserInfo = roomSessions.get(roomId);

		if (enteredUserInfo == null || enteredUserInfo.isEmpty()) {
			log.info("{} 유저는 방에 접속중이 아닙니다.", userId);
			return;
		}

		enteredUserInfo.remove(userId);

		if (enteredUserInfo.isEmpty()) {
			roomSessions.remove(roomId);
		}
	}

	// ====== 모든 방 exit 처리 ===========================================================================================================
	// 사용자가 보내는 WS 이벤트가 아니라 연결 종료 cleanup 내부 동작이야. 그래서 이건 private으로 둔다.
	public void removeSessionAllRooms(WebSocketSession session) {
		roomSessions.forEach((roomId, userMap) -> {
			userMap.entrySet().removeIf(entry -> entry.getValue().equals(session));

			if (userMap.isEmpty()) {
				roomSessions.remove(roomId);

				return;
			}
		});
	}

	// ====== userId PK로 WS 찾기 ===========================================================================================================
	public WebSocketSession findSessionByUserId(Long userId) {
		//	WebSocketSession userSession = connectedUserSessions.entrySet().stream().filter(entry -> Objects.equals(entry.getValue().getUserId(), userId))
		//	.map(Map.Entry::getKey).findFirst().orElse(null);
		WebSocketSession userSession = userIdToWsSessions.get(userId); // 역 직렬화도 해줘야, O(n) -> O(1)로 조회시간이 확 줄어든다.
		return userSession;
	}

	// ====== 현재 채팅방 접속중인 유저찾기 ===========================================================================================================
	public Set<Long> getViewingUserIds(Long roomId) {
		Map<Long, WebSocketSession> sessions = roomSessions.get(roomId);

		if (sessions == null || sessions.isEmpty()) {
			return Set.of();
		}

		sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());

		return new HashSet<>(sessions.keySet());
	}

	public Map<Long, WebSocketSession> getRoomSessions(Long roomId) {
		Map<Long, WebSocketSession> sessions = roomSessions.get(roomId);

		if (sessions == null || sessions.isEmpty()) {
			return Map.of();
		}

		sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());

		return sessions;
	}

	public int getConnectedSessionCount() {
		return wsSessionsToConnectedUser.size();
	}

	public int getActiveRoomCount() {
		return roomSessions.size();
	}

	public int getRoomViewingSessionCount() {
		return roomSessions.values().stream().mapToInt(Map::size).sum();
	}

}// GateWayWsSessionRegistry
