package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.domain.member.UserProfileResponseDTO;

public interface FriendQueryUseCase {
	List<UserProfileResponseDTO> getFriendList(Long userId);

	List<UserProfileResponseDTO> getReceivedFriendRequests(Long userId);

}
