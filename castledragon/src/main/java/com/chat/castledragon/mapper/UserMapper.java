package com.chat.castledragon.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.chat.castledragon.domain.UserDTO;

@Mapper
public interface UserMapper {
	List<UserDTO> friendList(String user_id);

	UserDTO getUser(String id);

}
