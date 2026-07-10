// 채팅 메시지 삭제 이벤트를 kafka로 전달하는 payload다.
package com.chat.contract.event.chatting;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDeletedEventDTO {
	private Long messageId;

	private Long roomId;
	private Long requesterUserId;
	private String requesterPublicId;

	private LocalDateTime deletedAt;
}
