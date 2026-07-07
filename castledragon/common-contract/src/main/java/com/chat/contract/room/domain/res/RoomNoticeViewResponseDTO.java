package com.chat.contract.room.domain.res;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomNoticeViewResponseDTO {
	private Long roomNoticeId;
	private Long roomId;

	private String roomNoticeAction;
	private String roomNoticeType;
	private String roomNoticeContents;
	private String roomNoticeStatus;

	private String requesterPublicId;
	private LocalDateTime lastAppliedAt;

}