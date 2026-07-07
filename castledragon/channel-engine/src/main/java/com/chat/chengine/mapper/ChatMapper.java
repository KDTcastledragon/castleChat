package com.chat.chengine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.domain.ChatMessagesDTO;
import com.chat.contract.room.domain.ChatRoomsDTO;
import com.chat.contract.room.domain.ChatUserLookupDTO;

@Mapper
public interface ChatMapper {
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

	ChatUserLookupDTO findUserInfoByPublicId(@Param("publicId") String publicId);

	ChatRoomsDTO findDirectRoom(@Param("user1") Long user1, @Param("user2") Long user2);

	int createRoom(ChatRoomsDTO dto);

	int insertRoomMember(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("role") String role, @Param("customRoomName") String customRoomName, @Param("customRoomThumbnail") String customRoomThumbnail, @Param("customRoomBackground") String customRoomBackground, @Param("memberStatus") String memberStatus);

	int reactivateRoomMembers(@Param("roomId") Long roomId, @Param("userIds") List<Long> userIds);

	List<ChatUserLookupDTO> findUserInfoByPublicIdList(@Param("publicIds") List<String> publicIds);

	int updateChatMessageAttachments(@Param("messageId") Long messageId, @Param("roomId") Long roomId, @Param("attachmentIds") List<Long> attachmentIds);

}
