package com.chat.castledragon.controller;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.castledragon.domain.FriendDTO;
import com.chat.castledragon.domain.SessionUserDTO;
import com.chat.castledragon.domain.UserProfileResponseDTO;
import com.chat.castledragon.service.FriendService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/friend")
@AllArgsConstructor
@Log4j2
public class FriendController {
	private final FriendService friendService;

	@PostMapping("/addFriend")
	public ResponseEntity<?> addFriend(@RequestBody FriendDTO requestData, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		try {
			boolean result = friendService.addFriend(me.getUserId(), requestData.getPublicId());

			if (!result) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("친구 요청 실패"); // 400
			}

			return ResponseEntity.ok().build();

		} catch (DuplicateKeyException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 친구이거나 친구 요청 중입니다."); // 409
		}
	}//addFri

	@GetMapping("/getFriendList")
	public ResponseEntity<?> getFriendList(HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 이미 여기서 꺼내고 있으니, @RequestParam("publicId") String data, 필요없다.

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		try {
			List<UserProfileResponseDTO> list = friendService.getFriendList(me.getUserId());

			log.info("{} 의 친구 목록 : {}", me.getUserId(), list);

			return ResponseEntity.ok(list);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no list");
		}
	}//getFriList

	@GetMapping("/getReceivedFriendRequests")
	public ResponseEntity<?> getReceivedFriendRequests(HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 이미 여기서 꺼내고 있으니, @RequestParam("publicId") String data 필요없다.

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		try {
			List<UserProfileResponseDTO> list = friendService.getReceivedFriendRequests(me.getUserId());

			log.info("{} 의 친구추가 요청 목록 : {}", me.getUserId(), list);

			return ResponseEntity.ok(list);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no list");
		}
	}//getRecvAddFriList

}//Controller 끝.
