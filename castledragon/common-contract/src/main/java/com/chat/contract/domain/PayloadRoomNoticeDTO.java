package com.chat.contract.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayloadRoomNoticeDTO {
	private Long roomId;
	private String noticeType;
	private String message;
	private LocalDateTime createdAt;
}