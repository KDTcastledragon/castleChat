package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.domain.UserProfileResponseDTO;

@Mapper
public interface FriendMapper {
	Long findUserIdByPublicId(@Param("targetPublicId") String targetPublicId);

	int addFriend(@Param("myUserId") Long myUserId, @Param("targetUserId") Long targetUserId);

	List<UserProfileResponseDTO> getFriendList(Long userId);

	List<UserProfileResponseDTO> getReceivedFriendRequests(Long userId);

	int acceptFriend(@Param("myUserId") Long myUserId, @Param("requesterUserId") Long requesterUserId);

	int respondFriendRequest(@Param("myUserId") Long myUserId, @Param("requesterUserId") Long requesterUserId, @Param("nextStatus") String nextStatus);
}
