package com.chat.castledragon.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.castledragon.domain.UserDTO;

@Mapper
public interface UserMapper {
	List<UserDTO> friendList(Long userId);

	UserDTO getUser(String id);

	String getUserLoginId(Long userId);

	int join(@Param("loginId") String loginId, @Param("password") String encodedPassword, @Param("nickname") String nickname, @Param("friendCode") String friendCode);

	int changePassword(String id, String encodedNewPassword);

	int withdrawMember(String id);

	List<UserDTO> allUsers();

}
