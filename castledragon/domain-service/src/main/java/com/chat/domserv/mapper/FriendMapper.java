package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.domain.UserProfileResponseDTO;

@Mapper
public interface FriendMapper {
	int addFriend(@Param("myUserId") Long myUserId, @Param("targetUserId") Long targetUserId);

	List<UserProfileResponseDTO> getFriendList(@Param("myUserId") Long myUserId);

	List<UserProfileResponseDTO> getReceivedFriendRequests(@Param("myUserId") Long myUserId);

	//	int acceptFriend(@Param("myUserId") Long myUserId, @Param("requesterUserId") Long requesterUserId);

	int respondFriendRequest(@Param("myUserId") Long myUserId, @Param("requesterUserId") Long requesterUserId, @Param("nextStatus") String nextStatus);
}
