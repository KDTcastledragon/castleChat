package com.chat.cmctr.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
// 테이블 DTO
public class ChatRoomsDTO {
	private Long roomId;

	private String roomType;
	private String roomStatus;
	private String roomName;
	private Long createdBy;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deactivatedAt;
}

//✔ChatRoomsDTO 쓰는곳 
//방생성 
//방상태 변경
//DB insert/update