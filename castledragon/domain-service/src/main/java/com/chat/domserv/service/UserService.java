package com.chat.domserv.service;

import java.util.List;

import com.chat.cmctr.dto.UserProfileResponseDTO;
import com.chat.domserv.domain.UserDTO;

public interface UserService {

	UserDTO getUser(String id);

	List<UserDTO> friendList(Long userId);

	UserDTO login(String id, String pw);

	List<UserDTO> allUsers();

	boolean changePassWord(String id, String newPw);

	boolean withdrawMember(String id);

	boolean join(String loginId, String password, String nickname);

	boolean loginIdDuplicateCheck(String id);

	List<UserProfileResponseDTO> searchUsers(String searchWord, Long userId);

	Long findUserIdByPublicId(String publicId);

}
