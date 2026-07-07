package com.chat.chengine.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FriendMapper {
	Long findUserIdByPublicId(@Param("publicId") String publicId);

	int addFriend(@Param("requesterUserId") Long requesterUserId, @Param("targetUserId") Long targetUserId);

	int respondFriendRequest(@Param("responderUserId") Long responderUserId, @Param("requesterUserId") Long requesterUserId, @Param("nextStatus") String nextStatus);
}