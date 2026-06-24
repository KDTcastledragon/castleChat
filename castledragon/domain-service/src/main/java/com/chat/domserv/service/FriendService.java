package com.chat.domserv.service;

import java.util.List;

import com.chat.cmctr.dto.UserProfileResponseDTO;

public interface FriendService {

	boolean addFriend(Long myUserId, String targetPublicId);

	List<UserProfileResponseDTO> getFriendList(Long userId);

	List<UserProfileResponseDTO> getReceivedFriendRequests(Long userId);

	boolean respondFriendRequest(Long userId, String publicId, String action);

}
