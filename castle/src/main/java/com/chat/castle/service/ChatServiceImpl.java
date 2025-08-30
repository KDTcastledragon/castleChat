package com.chat.castle.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chat.castle.domain.UserDTO;
import com.chat.castle.mapper.ChatMapper;

@Service
public class ChatServiceImpl implements ChatService {
	@Autowired
	ChatMapper chatmapper;

	@Override
	public List<UserDTO> allFriendsList(String user_id) {
		List<UserDTO> list = chatmapper.friendsList(user_id);
		return null;
	}

}
