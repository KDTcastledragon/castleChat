package com.chat.castledragon.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.castledragon.domain.ChatDTO;
import com.chat.castledragon.service.ChatService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/chat")
@AllArgsConstructor
@Log4j2
public class ChatController {
	ChatService chatservice;

	@PostMapping("/getChattingWithFriend")
	public ResponseEntity<?> getChattingWithFriend(@RequestBody Map<String, Object> data) {
		try {
			log.info("getChattingWithFriend" + data);
			String user_id = (String) data.get("user_id");
			String fri_id = (String) data.get("fri_id");

			List<ChatDTO> chatListWithFri = chatservice.getListWithFri(user_id, fri_id);

			return ResponseEntity.ok(chatListWithFri);

		} catch (Exception e) {
			throw e;
		}
	}

	@PostMapping("/sendMessage")
	public ResponseEntity<?> sendMessage(@RequestBody ChatDTO data) {
		try {
			log.info("sendMessage : " + data);
			String sender_id = data.getSender_id();
			String receiver_id = data.getReceiver_id();
			String msg = data.getMessage();

			chatservice.sendMessage(sender_id, receiver_id, msg);

			return ResponseEntity.ok("goodMessage");

		} catch (Exception e) {
			throw e;
		}
	}

}
