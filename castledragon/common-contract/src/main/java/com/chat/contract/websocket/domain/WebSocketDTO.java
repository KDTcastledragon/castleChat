package com.chat.contract.websocket.domain;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class WebSocketDTO {
	private String requestId;
	private String wsType;

	private Boolean isSuccess;
	private String wsMessage;

	private JsonNode payload;
}