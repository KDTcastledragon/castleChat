package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.chatting.domain.ChatAttachmentDTO;
import com.chat.contract.chatting.domain.res.ChatMessageReactionMemberResponseDTO;
import com.chat.contract.chatting.domain.res.ChatMessageReactionSummaryDTO;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.redis.RedisRoomMemberReadPositionDTO;
import com.chat.domserv.domain.MessageReaderCandidateDTO;

@Mapper
public interface DomServChatMapper {

	List<Long> findActiveRoomMemberIds(Long roomId);

	List<ChatMessageViewResponseDTO> loadMessagesInRoom(@Param("roomId") Long roomId, @Param("beforeMessageId") Long beforeMessageId, @Param("limit") int limit);

	List<ChatMessageViewResponseDTO> findMessageSenderPublicIds(@Param("roomId") Long roomId, @Param("messageIds") List<Long> messageIds);

	List<ChatAttachmentDTO> findChatAttachmentsByMessageIds(@Param("messageIds") List<Long> messageIds);

	List<ChatMessageReactionSummaryDTO> findReactionSummariesByMessageIds(@Param("messageIds") List<Long> messageIds, @Param("requesterUserId") Long requesterUserId);

	List<ChatMessageReactionMemberResponseDTO> findMessageReactionMembers(@Param("roomId") Long roomId, @Param("messageId") Long messageId);

	List<MessageReaderCandidateDTO> findMessageReaderCandidates(@Param("roomId") Long roomId, @Param("messageId") Long messageId);

	int countActiveRoomMember(@Param("roomId") Long roomId, @Param("userId") Long userId);

	List<RedisRoomMemberReadPositionDTO> findActiveRoomReadPositions(@Param("roomId") Long roomId);

	int insertChatAttachment(ChatAttachmentDTO dto);
}
