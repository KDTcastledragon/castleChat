package com.chat.contract.room.domain.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

//*** 하나의 broadcast 흐름으로 가기 위해, 두 개의 response dto를 조립하여 하나로 묶음.
public class RoomNoticeApplyResponseDTO {
	private RoomNoticeViewDTO roomNoticeView;
	private RoomFeedResponseDTO roomFeedResponse;
}
