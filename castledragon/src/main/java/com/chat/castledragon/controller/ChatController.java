package com.chat.castledragon.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.EnterGroupRequestDTO;
import com.chat.castledragon.domain.EnterRoomResponseDTO;
import com.chat.castledragon.domain.PayloadSendChatMessageResponseDTO;
import com.chat.castledragon.domain.SessionUserDTO;
import com.chat.castledragon.service.ChatService;
import com.chat.castledragon.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/chat")
@AllArgsConstructor
@Log4j2
public class ChatController {
	UserService userService;

	ChatService chatService;

	@PostMapping("/getOrCreateDirectRoom") // 무조건 “방(room)”을 먼저 만든다
	public ResponseEntity<?> getOrCreateDirectRoom(@RequestBody Map<String, Object> data, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.
		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		String friendPublicId = (String) data.get("friendPublicId");

		if (friendPublicId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("친구아이디없음.");
		}

		String nick = userService.getUser(friendPublicId).getNickname(); // 개발과정에서 임시로 씀. 추후 삭제.

		log.info("1:1 채팅방 입장 시도 : {} --> {} ", me.getNickname(), nick); // 개발과정에서 임시로 씀. 추후 삭제.

		//		Long friendUserId = userService.findUserIdByPublicId(friendPublicId);

		//		Long targetUserId = Long.valueOf(data.get("targetUserId").toString());

		EnterRoomResponseDTO roomInfo = chatService.getOrCreateDirectRoom(me, friendPublicId);

		return ResponseEntity.ok(roomInfo);
	}

	@PostMapping("/createGroupRoom") // 무조건 “방(room)”을 먼저 만든다
	public ResponseEntity<?> createGroupRoom(@RequestBody EnterGroupRequestDTO groupRoomData, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		if (groupRoomData.getSelectedFriendPublicIdList() == null) {
			log.info("초대인원없음 : {}", groupRoomData);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("초대인원없음.");
		}

		log.info("단톡방 생성 시도 : {} --> {} ", me.getNickname(), groupRoomData);

		EnterRoomResponseDTO roomInfo = chatService.createGroupRoom(me, groupRoomData.getRoomName(), groupRoomData.getRoomThumbnail(), groupRoomData.getSelectedFriendPublicIdList());

		log.info("GroupRoom roomInfo res : {}", roomInfo);
		return ResponseEntity.ok(roomInfo);
	}

	@GetMapping("loadMessagesInRoom/{roomId}")
	public List<PayloadSendChatMessageResponseDTO> getMessages(@PathVariable("roomId") Long roomId) { // @PathVariable : URL에 들어있는 값을 변수로 꺼내는 기능
		List<PayloadSendChatMessageResponseDTO> prevMessages = chatService.loadMessagesInRoom(roomId);
		log.info("prevMsg불러옴. : {}", prevMessages);
		return prevMessages;
	}

	@GetMapping("/getMyAllRooms")
	public ResponseEntity<?> getMyAllRooms(HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		List<ChatRoomListDTO> roomList = chatService.getMyAllRooms(me.getUserId());

		return ResponseEntity.ok(roomList);
	}
} // enterRoom 끝.

//@PostMapping("/updateLastRead") --> http 에서 ws로 read/send 이벤트를 처리할 것이기 때문에.
//public void updateLastRead(@RequestBody Map<String, Object> data) {
//
//	if (data == null) {
//		return;
//	}
//
//	Object roomObj = data.get("roomId");
//	Object userObj = data.get("userId");
//	Object lastObj = data.get("lastReadMessageId");
//
//	if (roomObj == null || userObj == null || lastObj == null) {
//		log.warn("updateLastRead null 데이터: {}", data);
//		return;
//	}
//
//	Long roomId = Long.valueOf(roomObj.toString());
//	Long userId = Long.valueOf(userObj.toString());
//	Long lastReadMessageId = Long.valueOf(lastObj.toString());
//
//	chatService.updateLastRead(roomId, userId, lastReadMessageId);
//}

//	@PostMapping("/updateLastRead")
//	public void updateLastRead(@RequestBody Map<String, Object> data) {
//
//		Long roomId = Long.valueOf(data.get("roomId").toString());
//		Long userId = Long.valueOf(data.get("userId").toString());
//		Long lastReadMessageId = Long.valueOf(data.get("lastReadMessageId").toString());
//
//		chatService.updateLastRead(roomId, userId, lastReadMessageId);
//	}
