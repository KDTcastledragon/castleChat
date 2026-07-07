package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.user.domain.UserProfileResponseDTO;

public interface FriendQueryUseCase {
	List<UserProfileResponseDTO> getFriendList(Long userId);

	List<UserProfileResponseDTO> getReceivedFriendRequests(Long userId);

}
