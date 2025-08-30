package com.chat.castle.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.chat.castle.domain.UserDTO;

@Mapper
public interface ChatMapper {

	List<UserDTO> friendsList(String user_id);

}
