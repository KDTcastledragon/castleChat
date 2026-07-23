package com.chat.aiassist.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.aiassist.domain.RefineMessageToneRequestDTO;
import com.chat.aiassist.domain.RefineMessageToneResponseDTO;
import com.chat.aiassist.domain.PersonalizedMessageRecommendRequestDTO;
import com.chat.aiassist.usecase.AiRecommendUseCase;
import com.chat.contract.user.domain.SessionUserDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 메시지 추천 HTTP 진입점. WS 안 씀 - 추천은 "요청->응답" 패턴이라 HTTP가 맞음. (aiAssistant.md 참고)
 */
@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/aiRecommend")
public class AiRecommendController {

	private final AiRecommendUseCase aiRecommendUseCase;

	@GetMapping("/ping")
	public String ping() {
		return "ai-assistant-service OK";
	}

	@GetMapping("/recommendMessages/{roomId}")
	public ResponseEntity<?> recommendMessages(@PathVariable("roomId") Long roomId, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		List<String> recommendations = aiRecommendUseCase.recommendMessages(me.getUserId(), roomId);

		log.info("{}에게 ai가 추천 : {}", me.getNickname(), recommendations);

		return ResponseEntity.ok(recommendations);
	}

	@PostMapping("/refineMessage")
	public ResponseEntity<?> refineMessageTone(@RequestBody RefineMessageToneRequestDTO request, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		String refinedMessage = aiRecommendUseCase.refineMessageTone(me.getUserId(), request.getMessageText(), request.getTone());

		log.info("{}의 메시지 말투 다듬기 완료. tone={}", me.getNickname(), request.getTone());

		return ResponseEntity.ok(new RefineMessageToneResponseDTO(refinedMessage));
	}

	@PostMapping("/recommendPersonalizedMessages")
	public ResponseEntity<?> recommendPersonalizedMessages(@RequestBody PersonalizedMessageRecommendRequestDTO request, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		List<String> recommendations = aiRecommendUseCase.recommendPersonalizedMessages(me.getUserId(), request.getRoomId(), request
				.getTargetPublicId(), request.getRelationshipType());

		log.info("{}의 섬세한 맞춤 메시지 추천 완료. room={}, target={}", me.getNickname(), request.getRoomId(), request.getTargetPublicId());

		return ResponseEntity.ok(recommendations);
	}

}
