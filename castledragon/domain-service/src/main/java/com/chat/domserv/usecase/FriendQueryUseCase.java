package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.domain.UserProfileResponseDTO;

public interface FriendQueryUseCase {
	List<UserProfileResponseDTO> getFriendList(Long userId);

	List<UserProfileResponseDTO> getReceivedFriendRequests(Long userId);

}
