//package com.chat.domserv.controller;
//
//import java.util.List;
//import java.util.Map;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.chat.contract.domain.ChatRoomListDTO;
//import com.chat.contract.domain.EnterGroupRequestDTO;
//import com.chat.contract.domain.EnterRoomResponseDTO;
//import com.chat.contract.domain.RoomIdRequestDTO;
//import com.chat.contract.domain.SessionUserDTO;
//import com.chat.domserv.service.UserService;
//import com.chat.domserv.usecase.ChatQueryUseCase;
//
//import jakarta.servlet.http.HttpSession;
//import lombok.AllArgsConstructor;
//import lombok.extern.log4j.Log4j2;
//
//@RestController
//@RequestMapping("/chat")
//@AllArgsConstructor
//@Log4j2
//public class ChatController {
//	UserService userService;
//	ChatQueryUseCase cqService;
//	//	interface는 사용하는 쪽이 구현체의 구체 클래스에 의존하지 않게 하는 계약이다.
//	//	Controller는 ChatServiceImpl인지, MockChatService인지, CachedChatService인지 몰라도 됨. ChatService 계약에 정의된 메서드만 호출하면 됨.
//	// 단, ChatService interface에 없는 impl구현체 고유 메서드는 controller가 호출할 수 없다.
//	// 현재 chat interface는 controller layer와 service layer의 경계가 된다.
//	// interface는 “사용하는 쪽이 구현체의 구체적인 존재를 몰라도 약속된 기능을 사용할 수 있게 하는 계약”이다.
//
//	//	실무적으로 interface가 의미 있으려면 보통 이유가 있어야 함.
//	//	1. 구현체 교체 가능성
//	//	2. 외부 기술 의존성 분리
//	//	3. 테스트 fake/mock 주입
//	//	4. use case 경계 명확화
//	//	5. 여러 구현체 전략 선택
//	//	6. module/process 간 계약 분리
//
//	@PostMapping("/getOrCreateDirectRoom") // 무조건 “방(room)”을 먼저 만든다
//	public ResponseEntity<?> getOrCreateDirectRoom(@RequestBody Map<String, Object> data, HttpSession session) {
//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.
//		if (me == null) {
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
//		}
//
//		String friendPublicId = (String) data.get("friendPublicId");
//
//		if (friendPublicId == null) {
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("친구아이디없음.");
//		}
//
//		String nick = userService.getUser(friendPublicId).getNickname(); // 개발과정에서 임시로 씀. 추후 삭제.
//
//		log.info("1:1 채팅방 입장 시도 : {} --> {} ", me.getNickname(), nick); // 개발과정에서 임시로 씀. 추후 삭제.
//
//		//		Long friendUserId = userService.findUserIdByPublicId(friendPublicId);
//
//		//		Long targetUserId = Long.valueOf(data.get("targetUserId").toString());
//
//		EnterRoomResponseDTO roomInfo = chatService.getOrCreateDirectRoom(me, friendPublicId);
//
//		return ResponseEntity.ok(roomInfo);
//	}
//
//	@PostMapping("/createGroupRoom") // 무조건 “방(room)”을 먼저 만든다
//	public ResponseEntity<?> createGroupRoom(@RequestBody EnterGroupRequestDTO groupRoomData, HttpSession session) {
//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.
//
//		if (me == null) {
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
//		}
//
//		if (groupRoomData.getSelectedFriendPublicIdList() == null) {
//			log.info("초대인원없음 : {}", groupRoomData);
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("초대인원없음.");
//		}
//
//		log.info("단톡방 생성 시도 : {} --> {} ", me.getNickname(), groupRoomData);
//
//		EnterRoomResponseDTO roomInfo = chatService.createGroupRoom(me, groupRoomData.getRoomName(), groupRoomData.getRoomThumbnail(), groupRoomData.getSelectedFriendPublicIdList());
//
//		log.info("GroupRoom roomInfo res : {}", roomInfo);
//		return ResponseEntity.ok(roomInfo);
//	}
//
//	@GetMapping("/enterExistedRoom/{roomId}")
//	public ResponseEntity<?> enterExistedRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.
//
//		if (me == null) {
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
//		}
//
//		EnterRoomResponseDTO roomInfo = chatService.enterExistedRoom(roomId, me);
//
//		return ResponseEntity.ok().body(roomInfo);
//	}
//
//	@GetMapping("/loadMessagesInRoom/{roomId}")
//	public List<PayloadSendChatMessageResponseDTO> getMessages(@PathVariable("roomId") Long roomId) { // @PathVariable : URL에 들어있는 값을 변수로 꺼내는 기능
//		List<PayloadSendChatMessageResponseDTO> loadedMessagesInRoom = chatService.loadMessagesInRoom(roomId);
//		log.info("loadMessagesInRoom불러옴. : {}", loadedMessagesInRoom);
//		return loadedMessagesInRoom;
//	}
//
//	@GetMapping("/getMyAllChatRooms")
//	public ResponseEntity<?> getMyAllChatRooms(HttpSession session) {
//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");
//
//		if (me == null) {
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
//		}
//
//		List<ChatRoomListDTO> roomList = chatService.getMyAllChatRooms(me.getUserId());
//
//		return ResponseEntity.ok(roomList);
//	}
//
//	@PostMapping("/leftRoom")
//	public ResponseEntity<?> leftRoom(@RequestBody RoomIdRequestDTO req, HttpSession session) {
//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");
//
//		if (me == null) {
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
//		}
//
//		chatService.leftRoom(req.getRoomId(), me);
//
//		return ResponseEntity.ok().build();
//	}
//} // enterRoom 끝.
//
////@PostMapping("/updateLastRead") --> http 에서 ws로 read/send 이벤트를 처리할 것이기 때문에.
////public void updateLastRead(@RequestBody Map<String, Object> data) {
////
////	if (data == null) {
////		return;
////	}
////
////	Object roomObj = data.get("roomId");
////	Object userObj = data.get("userId");
////	Object lastObj = data.get("lastReadMessageId");
////
////	if (roomObj == null || userObj == null || lastObj == null) {
////		log.warn("updateLastRead null 데이터: {}", data);
////		return;
////	}
////
////	Long roomId = Long.valueOf(roomObj.toString());
////	Long userId = Long.valueOf(userObj.toString());
////	Long lastReadMessageId = Long.valueOf(lastObj.toString());
////
////	chatService.updateLastRead(roomId, userId, lastReadMessageId);
////}
//
////	@PostMapping("/updateLastRead")
////	public void updateLastRead(@RequestBody Map<String, Object> data) {
////
////		Long roomId = Long.valueOf(data.get("roomId").toString());
////		Long userId = Long.valueOf(data.get("userId").toString());
////		Long lastReadMessageId = Long.valueOf(data.get("lastReadMessageId").toString());
////
////		chatService.updateLastRead(roomId, userId, lastReadMessageId);
////	}
