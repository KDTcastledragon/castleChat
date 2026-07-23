package com.chat.aiassist.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.aiassist.domain.RecentMessageDTO;

@Mapper
public interface AiAssistMapper {
	int countActiveRoomMember(@Param("roomId") Long roomId, @Param("userId") Long userId);

	List<RecentMessageDTO> findRecentMessagesInRoom(@Param("roomId") Long roomId, @Param("limit") int limit);

	Long findUserIdByPublicId(@Param("publicId") String publicId);

	String findRoomType(@Param("roomId") Long roomId);

	List<RecentMessageDTO> findSharedConversationMessages(@Param("requesterUserId") Long requesterUserId, @Param("targetUserId") Long targetUserId, @Param("limit") int limit);

}
