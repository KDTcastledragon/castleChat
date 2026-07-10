package com.chat.chengine.kafka;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.chat.chengine.config.ChatKafkaConfig;
import com.chat.contract.event.chatting.ChatMessageCreatedEventDTO;
import com.chat.contract.event.chatting.ChatMessageDeletedEventDTO;
import com.chat.contract.event.chatting.ChatMessageReactedEventDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 메시지 도메인 이벤트(created/deleted/reacted) durable 발행기.
 *
 * prpr 3 : response 시점은 DB CRUD 후가 아니라 kafka durable save 후다.
 * 여기서의 get(타임아웃) 대기가 그 "durable save 확인"이다. (acks=all 이므로
 * 브로커가 디스크에 기록을 마친 뒤에야 future가 완료된다.)
 * 발행 실패/타임아웃이면 예외 -> client는 처리 실패 응답을 받는다. (어디에도 저장 안 됨. 일관성 유지)
 *
 * key=roomId : 같은 방의 이벤트는 항상 같은 파티션 -> created -> deleted/reacted 소비 순서 보장.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class ChatMessageEventPublisher {

	// Boot 자동구성 KafkaTemplate은 <Object, Object> 타입. key는 LongSerializer 설정에 따라 roomId(Long)를 넣는다.
	private final KafkaTemplate<Object, Object> kafkaTemplate;

	@Value("${chat.kafka.send-timeout-ms:3000}")
	private long sendTimeoutMs;

	public void publishChatMessageCreated(ChatMessageCreatedEventDTO event) {
		publishDurable("sendMsg", event.getRoomId(), event.getMessageId(), event);
	}

	public void publishChatMessageDeleted(ChatMessageDeletedEventDTO event) {
		publishDurable("deleteMsg", event.getRoomId(), event.getMessageId(), event);
	}

	public void publishChatMessageReacted(ChatMessageReactedEventDTO event) {
		publishDurable("reactMsg", event.getRoomId(), event.getMessageId(), event);
	}

	private void publishDurable(String eventName, Long roomId, Long messageId, Object event) {
		try {
			SendResult<Object, Object> result = kafkaTemplate
					.send(ChatKafkaConfig.CHAT_MESSAGE_TOPIC, roomId, event)
					.get(sendTimeoutMs, TimeUnit.MILLISECONDS);

			log.info("[{}]kafka durable save 완료. msgId={}, room={}, partition={}, offset={}", eventName, messageId, roomId, result
					.getRecordMetadata().partition(), result.getRecordMetadata().offset());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("[{}]kafka 발행 인터럽트. msgId={}, room={}", eventName, messageId, roomId, e);
			throw new IllegalStateException("메시지 저장 실패(인터럽트)");
		} catch (Exception e) {
			log.error("[{}]kafka durable save 실패. msgId={}, room={}", eventName, messageId, roomId, e);
			throw new IllegalStateException("메시지 저장 실패");
		}
	}
}
