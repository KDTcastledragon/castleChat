// event worker의 kafka consumer 재시도와 dlt 이동 정책을 관리한다.
package com.chat.eventworker.config;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.chat.contract.kafka.ChatKafkaTopics;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class EventWorkerKafkaConfig {
	private static final int CHAT_EVENT_PARTITIONS = 6;
	private static final short REPLICATION_FACTOR = 1;

	@Bean
	public NewTopic chatEventTopic() {
		return TopicBuilder.name(ChatKafkaTopics.CHAT_EVENT_TOPIC)
				.partitions(CHAT_EVENT_PARTITIONS)
				.replicas(REPLICATION_FACTOR)
				.build();
	}

	@Bean
	public NewTopic chatEventDltTopic() {
		return TopicBuilder.name(ChatKafkaTopics.CHAT_EVENT_DLT)
				.partitions(CHAT_EVENT_PARTITIONS)
				.replicas(REPLICATION_FACTOR)
				.build();
	}

	@Bean
	public DefaultErrorHandler eventWorkerKafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> {
			log.error("kafka worker 최종 실패. dlt 이동. topic={}, partition={}, offset={}, key={}", record.topic(), record
					.partition(), record.offset(), record.key(), ex);

			return new TopicPartition(record.topic() + ".dlt", record.partition());
		});

		return new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 10L));
	}
}
