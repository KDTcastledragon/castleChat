package com.chat.contract.domain.member;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomMembersDTO {
	private Long roomMemberId;

	private Long roomId;
	private Long userId;

	private String role;

	private String customRoomName;
	private String customRoomThumbnail;
	private String customRoomBackground;

	private String memberStatus;

	private Long lastReadMessageId;
	private Long visibleAfterMessageId;

	private LocalDateTime joinedAt;
	private LocalDateTime leftAt;
	private LocalDateTime kickedAt;
	private LocalDateTime bannedAt;
	private LocalDateTime invitedAt;
	private LocalDateTime rejoinedAt;
}