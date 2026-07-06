package com.chat.wsgate.domain.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadApplyRoomNoticeRequestDTO {
	private Long roomId;

	private String roomNoticeAction; // CREATE / UPDATE / INACTIVATE / REACTIVATE / DELETE
	private Long targetRoomNoticeId; // CREATE=null, 그 외 필수

	private String roomNoticeType; // MESSAGE / CUSTOM
	private Long sourceMessageId;
	private String roomNoticeContents;
}

