package com.chat.aiassist.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class AiAssistExceptionHandler {

	@ExceptionHandler(AiRateLimitExceededException.class)
	public ResponseEntity<?> rateLimit(AiRateLimitExceededException e) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(Map.of("message", e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> badRequest(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("message", e.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<?> aiCallFail(IllegalStateException e) {
		log.error("AI assist 처리 실패", e);
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(Map.of("message", e.getMessage()));
	}
}
