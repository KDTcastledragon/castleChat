package com.chat.castledragon.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatDTO {
	private Long chat_id;
	private String sender_id;
	private String receiver_id;
	private String message;
	private LocalDateTime send_time;
	private Boolean is_read;
}
