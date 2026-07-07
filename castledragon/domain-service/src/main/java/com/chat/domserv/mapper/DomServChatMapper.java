package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.chatting.domain.ChatAttachmentDTO;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.redis.RedisRoomMemberReadPositionDTO;

@Mapper
public interface DomServChatMapper {

	List<Long> findActiveRoomMemberIds(Long roomId);

	List<ChatMessageViewResponseDTO> loadMessagesInRoom(@Param("roomId") Long roomId, @Param("beforeMessageId") Long beforeMessageId, @Param("limit") int limit);

	List<RedisRoomMemberReadPositionDTO> findActiveRoomReadPositions(@Param("roomId") Long roomId);

	int insertChatAttachment(ChatAttachmentDTO dto);
}
