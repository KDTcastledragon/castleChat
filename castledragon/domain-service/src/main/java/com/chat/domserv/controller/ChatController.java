package com.chat.domserv.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chat.contract.chatting.domain.ChatAttachmentDTO;
import com.chat.contract.user.domain.SessionUserDTO;
import com.chat.domserv.usecase.ChatCommandUseCase;
import com.chat.domserv.usecase.ChatQueryUseCase;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Log4j2
public class ChatController {
	private final ChatCommandUseCase chatCommandUseCase;
	private final ChatQueryUseCase chatQueryUseCase;

	@PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadChatAttachments(@RequestParam("roomId") Long roomId, @RequestParam("files") List<MultipartFile> files, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		List<ChatAttachmentDTO> uploadedAttachments = chatCommandUseCase.uploadChatAttachments(roomId, me, files);

		log.info("chat attachment uploaded. roomId={}, uploader={}, count={}", roomId, me.getUserId(), uploadedAttachments.size());

		return ResponseEntity.ok(uploadedAttachments);
	}

	@PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadCommonImage(@RequestParam("file") MultipartFile file, @RequestParam(value = "imageTarget", required = false) String imageTarget, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		String fileUrl = chatCommandUseCase.uploadCommonImage(me, file, imageTarget);

		log.info("common image uploaded. uploader={}, imageTarget={}, fileUrl={}", me.getUserId(), imageTarget, fileUrl);

		return ResponseEntity.ok(Map.of("fileUrl", fileUrl));
	}

	@GetMapping("/messages/{roomId}/{messageId}/reactions")
	public ResponseEntity<?> getMessageReactionMembers(@PathVariable("roomId") Long roomId, @PathVariable("messageId") Long messageId, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		return ResponseEntity.ok(chatQueryUseCase.findMessageReactionMembers(roomId, messageId, me.getUserId()));
	}

	@GetMapping("/messages/{roomId}/{messageId}/readers")
	public ResponseEntity<?> getMessageReaders(@PathVariable("roomId") Long roomId, @PathVariable("messageId") Long messageId, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		return ResponseEntity.ok(chatQueryUseCase.findMessageReaders(roomId, messageId, me.getUserId()));
	}

	@PostMapping("/messages/{roomId}/unreadCounts")
	public ResponseEntity<?> getMessageUnreadCounts(@PathVariable("roomId") Long roomId, @RequestBody List<Long> messageIds, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		return ResponseEntity.ok(chatQueryUseCase.findMessageUnreadCounts(roomId, messageIds, me.getUserId()));
	}
}
