package com.chat.chengine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.domain.ChatMessagesDTO;

@Mapper
public interface ChEngineChatMapper {
	int insertChatMessage(ChatMessagesDTO dto);

	List<Long> findAllActiveMemberIdsInRoom(Long roomId);

	Long findLastReadMessageId(@Param("roomId") Long roomId, @Param("userId") Long userId);

	int updateLastReadMessageId(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("lastReadMessageId") Long lastReadMessageId);

	Long lockChatMessageForUpdate(@Param("roomId") Long roomId, @Param("messageId") Long messageId);

	String findChatMessageStatus(@Param("roomId") Long roomId, @Param("messageId") Long messageId);

	Long findChatMessageSenderUserId(@Param("roomId") Long roomId, @Param("messageId") Long messageId);

	int deleteChatMessage(@Param("roomId") Long roomId, @Param("messageId") Long messageId, @Param("requesterUserId") Long requesterUserId);

	int insertChatMessageReaction(ReactChatMessageCommand cmd);

	int deleteChatMessageReaction(@Param("roomId") Long roomId, @Param("messageId") Long messageId, @Param("requesterUserId") Long requesterUserId, @Param("reactionCode") String reactionCode);
}
