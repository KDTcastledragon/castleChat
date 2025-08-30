package com.chat.castle.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@PostMapping("/friendsList")
	public ResponseEntity<?> friendsList(@RequestBody Map<String, Object> data) {
		try {
			log.info("ID for friendsList : " + data);
			String user_id = (String) data.get("user_id");
			List<UserDTO> list = chatservice.allFriendsList(user_id);

			return ResponseEntity.ok(list);
		} catch (Exception e) {
			throw e;
		}

	}

}
