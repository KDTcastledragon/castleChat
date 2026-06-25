package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.domain.UserProfileResponseDTO;
import com.chat.domserv.domain.UserDTO;

public interface UserQueryUseCase {

	boolean loginIdDuplicateCheck(String id);

	UserDTO getUser(String id);

	List<UserDTO> friendList(Long userId);

	List<UserProfileResponseDTO> searchUsers(String searchWord, Long userId);

	Long findUserIdByPublicId(String publicId);

	List<UserDTO> allUsers();
}
