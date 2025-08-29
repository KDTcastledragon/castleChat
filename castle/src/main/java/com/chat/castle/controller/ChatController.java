package com.chat.castle.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.castle.domain.UserDTO;
import com.chat.castle.service.ChatService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/chat")
@Log4j2
@AllArgsConstructor
public class ChatController {

	ChatService chatservice;

	@GetMapping("/friendsList")
	public ResponseEntity<?> friendsList() {
		try {
			log.info("friendsList");
			List<UserDTO> list = chatservice.allFriendsList();

			return ResponseEntity.ok(list);
		} catch (Exception e) {
			throw e;
		}

	}

}
