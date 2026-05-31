package com.chat.castledragon.service;

import java.util.List;

import com.chat.castledragon.domain.UserProfileResponseDTO;

public interface FriendService {

	boolean addFriend(Long myUserId, String targetPublicId);

	List<UserProfileResponseDTO> getFriendList(Long userId);

	List<UserProfileResponseDTO> getReceivedFriendRequests(Long userId);

}
