// chat 관련 kafka 이벤트를 db에 반영하기 위한 mapper다.
package com.chat.eventworker.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.domain.ChatMessagesDTO;

@Mapper
public interface EventPersistChatMapper {
	int insertChatMessage(ChatMessagesDTO dto);

	int updateChatMessageAttachments(@Param("messageId") Long messageId, @Param("roomId") Long roomId, @Param("attachmentIds") List<Long> attachmentIds);

	int deleteChatMessage(@Param("roomId") Long roomId, @Param("messageId") Long messageId, @Param("requesterUserId") Long requesterUserId);

	int insertChatMessageReaction(ReactChatMessageCommand cmd);

	int deleteChatMessageReaction(@Param("roomId") Long roomId, @Param("messageId") Long messageId, @Param("requesterUserId") Long requesterUserId, @Param("reactionCode") String reactionCode);
}
