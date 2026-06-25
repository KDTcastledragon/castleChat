package com.chat.domserv.controller;

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

import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ChatRoomListDTO;
import com.chat.contract.domain.EnterGroupRequestDTO;
import com.chat.contract.domain.EnterRoomResponseDTO;
import com.chat.contract.domain.RoomIdRequestDTO;
import com.chat.contract.domain.SessionUserDTO;
import com.chat.domserv.usecase.ChatQueryUseCase;
import com.chat.domserv.usecase.RoomCommandUseCase;
import com.chat.domserv.usecase.RoomQueryUseCase;
import com.chat.domserv.usecase.UserQueryUseCase;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/room")
@AllArgsConstructor
@Log4j2
public class RoomController {
	RoomCommandUseCase romCmdUseCase;
	RoomQueryUseCase romQryUseCase;
	UserQueryUseCase usrQryUseCase;
	ChatQueryUseCase ChtQryUseCase;

	public boolean sessionCheck(HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.

		if (me == null) {
			return false;
		}

		return true;
	}

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

		String nick = usrQryUseCase.getUser(friendPublicId).getNickname(); // 개발과정에서 임시로 씀. 추후 삭제.

		log.info("1:1 채팅방 입장 시도 : {} --> {} ", me.getNickname(), nick); // 개발과정에서 임시로 씀. 추후 삭제.

		//		Long friendUserId = userService.findUserIdByPublicId(friendPublicId);

		//		Long targetUserId = Long.valueOf(data.get("targetUserId").toString());

		EnterRoomResponseDTO roomInfo = romCmdUseCase.getOrCreateDirectRoom(me, friendPublicId);

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

		EnterRoomResponseDTO roomInfo = romCmdUseCase.createGroupRoom(me, groupRoomData.getRoomName(), groupRoomData.getRoomThumbnail(), groupRoomData.getSelectedFriendPublicIdList());

		log.info("GroupRoom roomInfo res : {}", roomInfo);
		return ResponseEntity.ok(roomInfo);
	}

	@GetMapping("/enterExistedRoom/{roomId}")
	public ResponseEntity<?> enterExistedRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		EnterRoomResponseDTO roomInfo = romQryUseCase.enterExistedRoom(roomId, me);

		return ResponseEntity.ok().body(roomInfo);
	}

	@GetMapping("/loadMessagesInRoom/{roomId}")
	public ResponseEntity<?> getMessages(@PathVariable("roomId") Long roomId, HttpSession session) { // @PathVariable : URL에 들어있는 값을 변수로 꺼내는 기능.
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		List<ChatMessageViewDTO> loadedMessagesInRoom = ChtQryUseCase.loadMessagesInRoom(roomId);
		log.info("loadMessagesInRoom불러옴. : {}", loadedMessagesInRoom);

		return ResponseEntity.ok(loadedMessagesInRoom);
	}

	@GetMapping("/getMyAllChatRooms")
	public ResponseEntity<?> getMyAllChatRooms(HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		List<ChatRoomListDTO> roomList = romQryUseCase.getMyAllChatRooms(me.getUserId());

		return ResponseEntity.ok(roomList);
	}

	@PostMapping("/leftRoom")
	public ResponseEntity<?> leftRoom(@RequestBody RoomIdRequestDTO req, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		romCmdUseCase.leftRoom(req.getRoomId(), me);

		return ResponseEntity.ok().build();
	}
}
