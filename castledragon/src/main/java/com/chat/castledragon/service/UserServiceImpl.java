package com.chat.castledragon.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chat.castledragon.domain.UserDTO;
import com.chat.castledragon.mapper.UserMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UserServiceImpl implements UserService {
	@Autowired
	UserMapper userMapper;

	@Autowired
	PasswordEncoder encoder;

	@Override
	public UserDTO getUser(String id) {
		UserDTO userPw = userMapper.getUser(id);
		return userPw;
	}

	@Override
	public UserDTO login(String id, String pw) {
		UserDTO userPw = userMapper.getUser(id);
		return userPw;
	}

	@Override
	public boolean loginIdDuplicateCheck(String loginId) {
		UserDTO existLoginId = userMapper.getUser(loginId);

		if (existLoginId == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean join(String loginId, String password, String nickname) {

		String encodedPassword = encoder.encode(password);

		int joinUser = userMapper.join(loginId, encodedPassword, nickname);

		log.info("뭐가문제지??? : {} {}", isJoined, encodedPassword);
		return isJoined > 0;

	}

	@Override
	public List<UserDTO> friendList(Long user_id) {
		List<UserDTO> list = userMapper.friendList(user_id);
		return list;
	}

	@Override
	public boolean changePassWord(String id, String newPw) {
		String encodedNewPassword = encoder.encode(newPw);

		int isChanged = userMapper.changePassword(id, encodedNewPassword);
		return isChanged > 0;
	}

	@Override
	public boolean withdrawMember(String id) {

		int isWithdrawed = userMapper.withdrawMember(id);

		return isWithdrawed > 0;
	}

	@Override
	public List<UserDTO> allUsers() {
		List<UserDTO> list = userMapper.allUsers();
		return list;
	}

}
