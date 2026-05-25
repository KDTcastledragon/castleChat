package com.chat.castledragon.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.castledragon.domain.ChatDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.EnterRoomResponseDTO;
import com.chat.castledragon.service.ChatService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/chat")
@AllArgsConstructor
@Log4j2
public class ChatController {
	ChatService chatService;

	@PostMapping("/enterRoom") // 무조건 “방(room)”을 먼저 만든다
	public ResponseEntity<EnterRoomResponseDTO> enterRoom(@RequestBody Map<String, Object> data) {
		log.info("■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■");
		log.info("{} --> {} 채팅 신청", data.get("senderId"), data.get("targetUserId"));

		Long senderId = Long.valueOf(data.get("senderId").toString());
		Long targetUserId = Long.valueOf(data.get("targetUserId").toString());

		EnterRoomResponseDTO roomInfo = chatService.enterRoom(senderId, targetUserId);

		return ResponseEntity.ok(roomInfo);
	}

	@GetMapping("getMessages/{roomId}")
	public List<ChatDTO> getMessages(@PathVariable("roomId") Long roomId) { // @PathVariable : URL에 들어있는 값을 변수로 꺼내는 기능
		List<ChatDTO> prevMessages = chatService.getMessages(roomId);
		return prevMessages;
	}

	@GetMapping("/myRooms/{userId}")
	public List<ChatRoomListDTO> getMyRooms(@PathVariable("userId") Long userId) {
		return chatService.getMyChatRooms(userId);
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
