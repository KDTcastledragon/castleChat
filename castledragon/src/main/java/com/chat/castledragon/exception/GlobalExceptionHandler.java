package com.chat.castledragon.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> illl() {

		return ResponseEntity.ok(null);
	}
}
