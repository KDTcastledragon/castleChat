package com.chat.castle.service;

import java.util.List;

import com.chat.castle.domain.UserDTO;

public interface ChatService {

	List<UserDTO> allFriendsList(String user_id);

}
