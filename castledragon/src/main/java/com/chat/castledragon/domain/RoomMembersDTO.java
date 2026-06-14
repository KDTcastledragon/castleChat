package com.chat.castledragon.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RoomMembersDTO {
	private Long roomMemberId;

	private Long roomId;
	private Long userId;

	private String role;

	private String customRoomName;
	private String customRoomThumbnail;
	private String memberStatus;

	private LocalDateTime joinedAt;
	private LocalDateTime leftAt;
	private LocalDateTime kickedAt;
	private LocalDateTime bannedAt;

	private Long lastReadMessageId;

}
