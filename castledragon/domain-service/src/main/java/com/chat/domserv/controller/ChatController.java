package com.chat.domserv.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chat.contract.domain.chatting.ChatAttachmentDTO;
import com.chat.contract.domain.user.SessionUserDTO;
import com.chat.domserv.usecase.ChatCommandUseCase;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Log4j2
public class ChatController {
	private final ChatCommandUseCase chatCommandUseCase;

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
}