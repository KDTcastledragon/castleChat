// 채팅 이벤트 kafka topic 이름을 공통으로 관리한다.
package com.chat.contract.kafka;

public final class ChatKafkaTopics {
	private ChatKafkaTopics() {
	}

	public static final String CHAT_EVENT_TOPIC = "castlechat.chat.message";
	public static final String CHAT_EVENT_DLT = "castlechat.chat.message.dlt";
}
