package com.chat.contract.chatting.domain.res;

import java.time.LocalDateTime;
import java.util.List;

import com.chat.contract.chatting.domain.ChatAttachmentDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageViewResponseDTO {
	private Long messageId;

	private Long roomId;
	private String senderPublicId;

	private String messageType;
	private String messageText;

	private Long replyToMessageId;
	private String messageStatus;

	private LocalDateTime createdAt;

	private Long unreadCount;

	private List<ChatAttachmentDTO> attachments;

	private List<ChatMessageReactionSummaryDTO> reactions;

	private List<Long> notificationTargetUserIds;
}

//unreadCount 
// int가 아닌 Long으로 굳이 하는 이유는? SQL의 COUNT(*)는 보통 BIGINT타입으로 처리됨. JAVA로 오면 Long타입으로 매핑된다. 안정성을 위해 int(x) Long(o)
// unreadCount DB저장하지 않는 이유 : 누가 읽을 때마다 모든 message unreadCount 갱신. 10000개의 메세지를 누군가 읽었다면? 9999개 update 발생. 최악임.
