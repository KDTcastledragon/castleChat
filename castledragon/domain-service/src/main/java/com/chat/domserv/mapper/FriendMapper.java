package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.user.domain.UserProfileResponseDTO;

@Mapper
public interface FriendMapper {
	List<UserProfileResponseDTO> getFriendList(@Param("myUserId") Long myUserId);

	List<UserProfileResponseDTO> getReceivedFriendRequests(@Param("myUserId") Long myUserId);
}
