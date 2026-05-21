package com.chat.castledragon.domain;

import java.time.LocalDateTime;

import lombok.Data;

@Data
// 화면표시용 DTO
public class ChatRoomListDTO {
	private Long roomId;

	private Long targetUserId;

	private String targetLoginId;

	private Long unreadCount;

	private String lastMessage;

	private LocalDateTime lastMessageTime;
}

//ChatRoomListDTO 쓰는 곳
//채팅방 리스트 화면
//unread count
//last message