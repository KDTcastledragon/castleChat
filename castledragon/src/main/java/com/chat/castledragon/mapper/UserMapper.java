package com.chat.castledragon.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.castledragon.domain.ChatMemberDTO;
import com.chat.castledragon.domain.UserDTO;
import com.chat.castledragon.domain.UserProfileResponseDTO;

@Mapper
public interface UserMapper {

	UserDTO getUserByLoginId(String loginId);

	UserDTO getUser(String id);

	Long findUserIdByPublicId(@Param("publicId") String publicId);

	String getUserLoginId(Long userId);

	int join(@Param("publicId") String publicId, @Param("loginId") String loginId, @Param("password") String password, @Param("nickname") String nickname, @Param("friendCode") String friendCode);

	int changePassword(String id, String encodedNewPassword);

	int withdrawMember(String id);

	List<UserDTO> allUsers();

	List<UserProfileResponseDTO> searchUsers(@Param("searchWord") String searchWord, @Param("userId") Long userId);

	List<UserDTO> friendList(Long userId);

	ChatMemberDTO findUserInfoByPublicId(String publicId);

}
