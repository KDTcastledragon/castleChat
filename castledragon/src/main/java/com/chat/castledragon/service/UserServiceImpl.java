package com.chat.castledragon.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chat.castledragon.domain.UserDTO;
import com.chat.castledragon.mapper.UserMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UserServiceImpl implements UserService {
	@Autowired
	UserMapper usermapper;

	@Override
	public List<UserDTO> friendList(String user_id) {
		List<UserDTO> list = usermapper.friendList(user_id);
		return list;
	}

	@Override
	public UserDTO login(String id, String pw) {
		UserDTO userPw = usermapper.getUser(id);
		return userPw;
	}

}
