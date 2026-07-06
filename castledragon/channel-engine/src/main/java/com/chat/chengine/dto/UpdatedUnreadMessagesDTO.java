package com.chat.chengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatedUnreadMessagesDTO {
	private Long messageId;
	private Long unreadCount;
}


