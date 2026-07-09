package com.chat.chengine.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * kafka "castlechat.chat.message" 토픽의 메시지 삭제 이벤트 페이로드.
 * 검증(작성자/상태)은 ChatCommandService에서 동기로 끝났고, consumer(ChatMessagePersistWorker)는 조건부 UPDATE만 수행한다.
 * 같은 토픽 + key=roomId 라서 created 이벤트보다 늦게 소비됨이 보장된다(파티션 내 순서 보장).
 */
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
