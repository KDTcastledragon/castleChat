package com.chat.chengine.domain;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * kafka "castlechat.chat.message.created" 이벤트 페이로드.
 * producer(ChatMessageEventPublisher)가 발행하고 ChatMessagePersistWorker가 소비하여 DB insert 한다.
 * messageId는 발행 전에 ChatMessageIdGenerator로 이미 확정된 값이다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageCreatedEventDTO {
	private Long messageId;

	private Long roomId;
	private Long senderUserId;
	private String senderPublicId;

	private String messageType;
	private String messageText;

	private Long replyToMessageId;

	private List<Long> attachmentIds;

	private LocalDateTime createdAt;
}
