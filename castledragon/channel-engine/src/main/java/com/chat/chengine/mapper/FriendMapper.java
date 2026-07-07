package com.chat.chengine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FriendMapper {
	Long findUserIdByPublicId(@Param("publicId") String publicId);

	String findNicknameByUserId(@Param("userId") Long userId);

	int addFriend(@Param("requesterUserId") Long requesterUserId, @Param("targetUserId") Long targetUserId);

	int respondFriendRequest(@Param("responderUserId") Long responderUserId, @Param("requesterUserId") Long requesterUserId, @Param("nextStatus") String nextStatus);

	List<Long> findAcceptedFriendUserIds(@Param("userId") Long userId);
}
