package com.chat.domserv.usecase;

public interface FriendCommandUseCase {

	boolean addFriend(Long myUserId, String targetPublicId);

	boolean respondFriendRequest(Long userId, String publicId, String action);

}
