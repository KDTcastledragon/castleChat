package com.chat.contract.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomMemberReadPositionDTO {
	private Long userId;
	private String publicId;
	private Long lastReadMessageId;
	private Long visibleAfterMessageId; // 다음 단계 prevMsg urc 재계산에서 바로 쓸 거라 지금 같이 넣어두는 게 좋음.
}