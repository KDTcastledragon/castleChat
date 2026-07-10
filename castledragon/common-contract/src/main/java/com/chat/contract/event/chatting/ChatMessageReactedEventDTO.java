// 채팅 메시지 리액션 이벤트를 kafka로 전달하는 payload다.
package com.chat.contract.event.chatting;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageReactedEventDTO {
	private Long roomId;
	private Long messageId;

	private Long requesterUserId;
	private String requesterPublicId;

	private String reactionType;
	private String reactionCode;

	private Boolean addRequested;

	private LocalDateTime reactedAt;
}
