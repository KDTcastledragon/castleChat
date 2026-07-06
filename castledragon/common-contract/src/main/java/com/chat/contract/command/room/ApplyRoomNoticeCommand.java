package com.chat.contract.command.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplyRoomNoticeCommand {
	private Long roomId;
	private String roomNoticeAction; // CREATE / UPDATE / INACTIVATE / REACTIVATE / DELETE
	private Long targetRoomNoticeId; // create : auto_incre 라서 입력 불가능. null.  ,   그 외 : 값 입력 필수. not null.
	private String roomNoticeType; // MESSAGE / CUSTOM

	private Long sourceMessageId;
	private String roomNoticeContents;// ACTIVE / INACTIVATE / DELETE

	private Long requesterUserId;
	private String requesterPublicId;
}
