package com.chat.castledragon.service;

import java.util.List;

import com.chat.castledragon.domain.UserDTO;

public interface UserService {

	List<UserDTO> friendList(Long userId);

	UserDTO login(String id, String pw);

	List<UserDTO> allUsers();

	boolean changePassWord(String id, String newPw);

	boolean withdrawMember(String id);

	boolean join(String loginId, String password, String nickname);

	boolean loginIdDuplicateCheck(String id);

	UserDTO getUser(String id);

}
