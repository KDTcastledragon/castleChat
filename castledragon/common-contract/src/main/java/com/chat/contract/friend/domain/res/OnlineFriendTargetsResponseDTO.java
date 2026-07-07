package com.chat.contract.friend.domain.res;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlineFriendTargetsResponseDTO {
	private Long userId;
	private List<Long> targetUserIds;
}
