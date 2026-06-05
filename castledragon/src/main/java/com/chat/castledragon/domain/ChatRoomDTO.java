package com.chat.castledragon.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
// 테이블 DTO
public class ChatRoomDTO {
	private Long roomId;

	private String roomType;
	private String roomName;

	private Long createdBy;

	private String roomStatus;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

//✔ChatRoomsDTO 쓰는곳 
//방생성 
//방상태 변경
//DB insert/update