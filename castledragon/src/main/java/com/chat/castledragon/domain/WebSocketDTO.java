package com.chat.castledragon.domain;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class WebSocketDTO {
	private String requestId;
	private String socketType;

	private Boolean success;
	private String wsMessage;

	private JsonNode payload;
}