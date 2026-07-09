package com.chat.chengine.domain;

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


