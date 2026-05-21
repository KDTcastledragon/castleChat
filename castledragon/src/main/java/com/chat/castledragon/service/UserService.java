package com.chat.castledragon.service;

import java.util.List;

import com.chat.castledragon.domain.UserDTO;

public interface UserService {

	List<UserDTO> friendList(Long userId);

	UserDTO login(String id, String pw);

	List<UserDTO> allUsers();

}
