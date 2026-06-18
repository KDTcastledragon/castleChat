package com.chat.castledragon.monitoring;

import org.springframework.stereotype.Component;

import com.chat.castledragon.websocket.WsSessionRegistry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ChatMetrics {

	private final Counter sendMessageCounter;
	private final Counter readMessageCounter;
	private final Counter broadcastFailCounter;

	public ChatMetrics(MeterRegistry meterRegistry, WsSessionRegistry wsSessionRegistry) {

		Gauge.builder("chat.ws.connected.sessions", wsSessionRegistry, WsSessionRegistry::getConnectedSessionCount).description("현재 WebSocket 연결 수").register(meterRegistry);

		Gauge.builder("chat.room.active.count", wsSessionRegistry, WsSessionRegistry::getActiveRoomCount).description("현재 활성 roomSession 방 수").register(meterRegistry);

		Gauge.builder("chat.room.viewing.sessions", wsSessionRegistry, WsSessionRegistry::getRoomViewingSessionCount).description("현재 채팅방을 보고 있는 user-room 관계 수").register(meterRegistry);

		sendMessageCounter = Counter.builder("chat.send.message").description("처리된 채팅 메시지 수").register(meterRegistry);

		readMessageCounter = Counter.builder("chat.read.message").description("처리된 읽음 이벤트 수").register(meterRegistry);

		broadcastFailCounter = Counter.builder("chat.broadcast.fail").description("WebSocket broadcast 실패 수").register(meterRegistry);
	}

	public void incrementSendMessage() {
		sendMessageCounter.increment();
	}

	public void incrementReadMessage() {
		readMessageCounter.increment();
	}

	public void incrementBroadcastFail() {
		broadcastFailCounter.increment();
	}
}