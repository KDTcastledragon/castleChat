package com.chat.contract.room.domain.res;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomFeedResponseDTO {
	private Long roomId;

	private String feedType; // LEFT / INVITE / KICK / BAN / ROLE_CHANGED

	private String requesterPublicId;
	private String requesterNickname;

	private List<String> targetPublicIds;
	private List<String> targetNicknames;

	private String targetRole;

	private String feedText;

	private LocalDateTime feedAt;
}
