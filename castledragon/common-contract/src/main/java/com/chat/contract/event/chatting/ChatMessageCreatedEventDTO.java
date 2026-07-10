// 채팅 메시지 생성 이벤트를 kafka로 전달하는 payload다.
package com.chat.contract.event.chatting;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
