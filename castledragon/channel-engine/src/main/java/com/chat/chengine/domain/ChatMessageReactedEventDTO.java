package com.chat.chengine.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * kafka "castlechat.chat.message" 토픽의 리액션 추가/취소 이벤트 페이로드.
 * addRequested=true -> INSERT IGNORE / false -> DELETE. 양쪽 다 멱등이라 재전달 안전.
 */
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
