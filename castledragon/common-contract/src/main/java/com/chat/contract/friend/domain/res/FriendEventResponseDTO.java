package com.chat.contract.friend.domain.res;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendEventResponseDTO {
	private String friendEventType;

	private Long requesterUserId;
	private String requesterPublicId;

	private Long targetUserId;
	private String targetPublicId;

	private String friendStatus;
	private LocalDateTime eventAt;
}