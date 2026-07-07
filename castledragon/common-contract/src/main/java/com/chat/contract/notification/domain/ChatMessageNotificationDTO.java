package com.chat.contract.notification.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageNotificationDTO {
	private Long roomId;
	private Long messageId;

	private String senderPublicId;
	private String senderNickname;
	private String senderProfileImg;

	private String messageType;
	private String previewText;

	private LocalDateTime notifiedAt;
}
