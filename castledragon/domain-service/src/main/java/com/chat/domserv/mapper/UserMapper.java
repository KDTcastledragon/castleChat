package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.domain.member.ChatUserLookupDTO;
import com.chat.contract.domain.member.UserProfileResponseDTO;
import com.chat.domserv.domain.UserDTO;

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

	ChatUserLookupDTO findUserInfoByPublicId(String publicId);

	List<ChatUserLookupDTO> findUserInfoByPublicIdList(@Param("publicIds") List<String> publicIds);

}
