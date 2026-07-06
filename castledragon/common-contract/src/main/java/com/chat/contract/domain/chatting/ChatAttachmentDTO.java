package com.chat.contract.domain.chatting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatAttachmentDTO {
	private Long attachmentId;

	private Long messageId;
	private Long roomId;
	private Long uploaderUserId;

	private String fileUrl;
	private String originalFileName;
	private String contentType;
	private Long fileSize;

	private String attachmentKind;
	private String attachmentStatus;

	private Integer width;
	private Integer height;
	private Long durationMs;

	private Integer sortOrder;
}