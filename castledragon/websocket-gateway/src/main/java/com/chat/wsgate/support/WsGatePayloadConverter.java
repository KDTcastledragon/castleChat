package com.chat.wsgate.support;

import org.springframework.stereotype.Component;

import com.chat.contract.domain.websocket.WebSocketDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WsGatePayloadConverter {
	private final ObjectMapper objMapper;

	public <T> T convert(WebSocketDTO dto, Class<T> clazz) {
		return objMapper.convertValue(dto.getPayload(), clazz);
	}
}
