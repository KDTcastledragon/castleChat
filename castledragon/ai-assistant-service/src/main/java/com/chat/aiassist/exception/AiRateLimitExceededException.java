package com.chat.aiassist.exception;

public class AiRateLimitExceededException extends RuntimeException {

	public AiRateLimitExceededException(String message) {
		super(message);
	}
}
