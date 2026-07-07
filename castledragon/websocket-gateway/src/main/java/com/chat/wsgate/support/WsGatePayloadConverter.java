package com.chat.wsgate.support;

import org.springframework.stereotype.Component;

import com.chat.contract.websocket.domain.WebSocketDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WsGatePayloadConverter {
	private final ObjectMapper objMapper;

	public <T> T convert(WebSocketDTO dto, Class<T> clazz) {
		return objMapper.convertValue(dto.getPayload(), clazz);
	}

	// WebSocketDTO 안의 payload를 각각의 메소드에 알맞게 Payload DTO 타입으로 변환하는 공통 함수.
	// <T> : 이 메서드는 호출할 때 원하는 타입을 받아서, 그 타입으로 변환해서 돌려줄 수 있다.
	// Java는 런타임에 제네릭 타입 정보를 잃는 부분이 있습니다. 그래서 T만 보고는 Jackson이 “무슨 클래스로 바꿔야 하는지” 모릅니다. 
	// 그래서 Class<T> clazz를 이용하여 명시적으로 클래스 정보를 넘깁니다. 즉, 'payload를 어떤 클래스 타입으로 변환할지 알려주는 인자'이다.
	// convertValue : 이미 Java 객체/JsonNode/Map 형태로 존재하는 값을 다른 Java 객체 타입으로 변환해주는 메소드. 이미 객체 형태인 값을 '다른 객체'로 바꿉니다.
	// ws의 payload는 JsonNode입니다. 그래서, dto.getPayload()는 문자열이 아니라 JsonNode이다. --> JsonNode를 EnterRoomPayloadDTO 로 바꾸는 겁니다.
	// Jackson은 사람이 아니라 Java에서 JSON을 다루는 대표 라이브러리 이름입니다. 오타 아니에요.
}
