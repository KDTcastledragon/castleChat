package com.chat.domserv.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chat.contract.domain.UserProfileResponseDTO;
import com.chat.domserv.domain.UserDTO;
import com.chat.domserv.mapper.UserMapper;
import com.chat.domserv.usecase.UserQueryUseCase;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UserQueryService implements UserQueryUseCase {
	@Autowired
	UserMapper userMapper;

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
	public List<UserProfileResponseDTO> searchUsers(String searchWord, Long userId) {
		List<UserProfileResponseDTO> list = userMapper.searchUsers(searchWord, userId);
		return list;
	}

	@Override
	public Long findUserIdByPublicId(String publicId) {
		Long userId = userMapper.findUserIdByPublicId(publicId);
		return userId;
	}

	@Override
	public UserDTO getUser(String id) {
		UserDTO userPw = userMapper.getUser(id);
		return userPw;
	}

	@Override
	public List<UserDTO> allUsers() {
		List<UserDTO> list = userMapper.allUsers();
		return list;
	}

}
