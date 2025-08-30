package com.chat.castledragon.service;

import java.util.List;

import com.chat.castledragon.domain.UserDTO;

public interface UserService {

	List<UserDTO> friendList(String user_id);

	UserDTO login(String id, String pw);

}
