package com.chat.contract.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor // Jackson이 받아야 하니까 @NoArgsConstructor 필요해. --> 먼소리지? 
public class EnterGroupRequestDTO {
	private String roomName;
	private String roomThumbnail;
	private List<String> selectedFriendPublicIdList;
}
