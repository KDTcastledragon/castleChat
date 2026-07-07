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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.room.domain.ChatRoomListDTO;
import com.chat.contract.room.domain.EnterGroupRequestDTO;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.user.domain.SessionUserDTO;
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

	//	private SessionUserDTO getLoginUser(HttpSession session) {
	//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");
	//
	//		if (me == null) {
	//			throw new IllegalStateException("로그인 필요");
	//		}
	//
	//		return me;
	//	}

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

		EnterRoomResponseDTO roomInfo = romCmdUseCase
				.createGroupRoom(me, groupRoomData.getRoomName(), groupRoomData.getRoomThumbnail(), groupRoomData.getSelectedFriendPublicIdList());

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
	public ResponseEntity<?> getMessages(@PathVariable("roomId") Long roomId, @RequestParam(value = "beforeMessageId", required = false) Long beforeMessageId, @RequestParam(value = "limit", defaultValue = "50") int limit, HttpSession session) {

		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		log.info("loadMessagesInRoom 호출: user={}, roomId={}, beforeMessageId={}, limit={}", me.getNickname(), roomId, beforeMessageId, limit);

		List<ChatMessageViewResponseDTO> loadedMessagesInRoom = ChtQryUseCase.loadMessagesInRoom(roomId, beforeMessageId, limit);

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
	//
	//	@PostMapping("/inviteGroupRoom")
	//	public ResponseEntity<?> inviteRoom(@RequestBody InviteMemberInRoomRequestDTO requestInviteData, HttpSession session) {
	//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");
	//
	//		if (me == null) {
	//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
	//		}
	//
	//		int invitedCount = romCmdUseCase.inviteGroupRoom(requestInviteData.getRoomId(), me, requestInviteData.getInviteTargetMemberPublicIds());
	//		return ResponseEntity.ok(Map.of("invitedCount", invitedCount));
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
	//		Boolean isLefted = romCmdUseCase.leftRoom(req.getRoomId(), me);
	//
	//		if (isLefted == false) {
	//			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("실패");
	//		}
	//
	//		return ResponseEntity.ok().build();
	//	}
	//
	//	@PostMapping("/kickMemberInRoom")
	//	public ResponseEntity<?> kickMemberInRoom(@RequestBody KickMemberInRoomRequestDTO requestKickData, HttpSession session) {
	//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");
	//
	//		if (me == null) {
	//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
	//		}
	//
	//		int kickedCount = romCmdUseCase.kickMemberInRoom(requestKickData.getRoomId(), me, requestKickData.getKickTargetPublicId());
	//
	//		return ResponseEntity.ok(Map.of("kickedCount", kickedCount));
	//	}
	//
	//	@PostMapping("/banMemberInRoom")
	//	public ResponseEntity<?> banMemberInRoom(@RequestBody KickMemberInRoomRequestDTO requestBanData, HttpSession session) {
	//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");
	//
	//		if (me == null) {
	//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
	//		}
	//
	//		int bannedCount = romCmdUseCase.banMemberInRoom(requestBanData.getRoomId(), me, requestBanData.getKickTargetPublicId());
	//
	//		return ResponseEntity.ok(Map.of("bannedCount", bannedCount));
	//	}
	//
	//	@PostMapping("/changeMemberRoleInRoom")
	//	public ResponseEntity<?> changeMemberRoleInRoom(@RequestBody ChangeMemberRoleInRoomRequestDTO requestChgData, HttpSession session) {
	//		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");
	//
	//		if (me == null) {
	//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
	//		}
	//
	//		int chgCount = romCmdUseCase.changeMemberRoleInRoom(requestChgData.getRoomId(), me, requestChgData.getTargetPublicId());
	//
	//		return ResponseEntity.ok(Map.of("bannedCount", chgCount));
	//	}

}
