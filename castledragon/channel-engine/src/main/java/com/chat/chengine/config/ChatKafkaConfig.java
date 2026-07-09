package com.chat.chengine.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.log4j.Log4j2;

/**
 * sendMessage kafka 비동기 처리용 설정.
 * - 토픽 자동 생성 (앱 기동 시 KafkaAdmin이 브로커에 생성. 이미 있으면 무시)
 * - consumer 재시도/DLT 에러핸들러
 *
 * producer/consumer의 직렬화, acks 등은 application.properties의 spring.kafka.* 로 설정한다.
 */
@Configuration
@Log4j2
public class ChatKafkaConfig {

	// 메시지 도메인 이벤트 스트림 (created/deleted/reacted 3종이 한 토픽에 흐른다)
	// 한 토픽 + key=roomId 이어야 같은 방의 create -> delete/react 소비 순서가 보장된다(파티션 내 순서 보장).
	public static final String CHAT_MESSAGE_TOPIC = "castlechat.chat.message";
	public static final String CHAT_MESSAGE_DLT = "castlechat.chat.message.dlt";

	// 같은 roomId(key)는 항상 같은 파티션 -> 방 단위 순서 보장.
	// 파티션 증설은 key->파티션 매핑을 바꾸므로 초기값을 넉넉히. (MVP 기준 6)
	private static final int CHAT_MESSAGE_PARTITIONS = 6;

	// 로컬 단일 브로커 기준 1. 다중 브로커 전환 시 3으로 변경할 것. (설계문서 G항목 참고)
	private static final short REPLICATION_FACTOR = 1;

	@Bean
	public NewTopic chatMessageTopic() {
		return TopicBuilder.name(CHAT_MESSAGE_TOPIC)
				.partitions(CHAT_MESSAGE_PARTITIONS)
				.replicas(REPLICATION_FACTOR)
				.build();
	}

	@Bean
	public NewTopic chatMessageDltTopic() {
		return TopicBuilder.name(CHAT_MESSAGE_DLT)
				.partitions(CHAT_MESSAGE_PARTITIONS)
				.replicas(REPLICATION_FACTOR)
				.build();
	}

	/**
	 * consumer 처리 실패 시 : 0.5초 간격 10회 재시도 -> 최종 실패 시 DLT로 원본 이벤트 발행.
	 * startDirectChat/startGroupChat의 방 생성 tx가 commit 되기 전에 이벤트가 먼저 소비되는 경우도
	 * 이 재시도 구간(약 5초) 안에서 흡수된다.
	 */
	@Bean
	public DefaultErrorHandler chatKafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> {
			log.error("kafka consumer 최종 실패. DLT로 이동. topic={}, partition={}, offset={}, key={}", record.topic(), record
					.partition(), record.offset(), record.key(), ex);

			return new org.apache.kafka.common.TopicPartition(record.topic() + ".dlt", record.partition());
		});

		return new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 10L));
	}
}
