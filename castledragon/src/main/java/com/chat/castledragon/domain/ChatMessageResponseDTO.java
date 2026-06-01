package com.chat.castledragon.domain;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatMessageResponseDTO {
	private Long messageId;

	private Long roomId;
	private String senderPublicId; // 메시지를 보낸 사람의 publicId

	private String msgText;

	private LocalDateTime createdAt; // default.

	private Long lastReadMessageId;

	private Long unreadCount; // int가 아닌 Long으로 굳이 하는 이유는? SQL의 COUNT(*)는 보통 BIGINT타입으로 처리됨. JAVA로 오면 Long타입으로 매핑된다. 안정성을 위해 int(x) Long(o)
	// unreadCount DB저장하지 않는 이유 : 누가 읽을 때마다 모든 message unreadCount 갱신. 10000개의 메세지를 누군가 읽었다면? 9999개 update 발생. 최악임.
}
