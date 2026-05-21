package com.chat.castledragon.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.chat.castledragon.domain.UserDTO;

@Mapper
public interface UserMapper {
	List<UserDTO> friendList(Long userId);

	UserDTO getUser(String id);

	String getUserLoginId(Long userId);

	List<UserDTO> allUsers();

}
