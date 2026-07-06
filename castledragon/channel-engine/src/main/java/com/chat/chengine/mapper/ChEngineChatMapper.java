package com.chat.chengine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.domain.chatting.ChatMessagesDTO;

@Mapper
public interface ChEngineChatMapper {
	int insertChatMessage(ChatMessagesDTO dto);

	List<Long> findAllActiveMemberIdsInRoom(Long roomId);

	Long findLastReadMessageId(@Param("roomId") Long roomId, @Param("userId") Long userId);

	int updateLastReadMessageId(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("lastReadMessageId") Long lastReadMessageId);
}


