package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.cache.RedisRoomMemberReadPositionDTO;
import com.chat.contract.domain.chatting.ChatAttachmentDTO;
import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.UpdatedUnreadMessagesDTO;

@Mapper
public interface ChatMapper {

	List<Long> findActiveRoomMemberIds(Long roomId);

	List<ChatMessageViewResponseDTO> loadMessagesInRoom(@Param("roomId") Long roomId, @Param("beforeMessageId") Long beforeMessageId, @Param("limit") int limit);

	List<RedisRoomMemberReadPositionDTO> findActiveRoomReadPositions(@Param("roomId") Long roomId);

	//	===================================================================================================
	List<UpdatedUnreadMessagesDTO> getUpdatedUnreadCountChatMessages(@Param("roomId") Long roomId, @Param("oldLastReadMsgId") Long oldLastReadMsgId, @Param("newLastReadMessageId") Long newLastReadMessageId);

	Long lockRoomForUpdate(@Param("roomId") Long roomId);

	void decreaseActiveMemberCount(@Param("roomId") Long roomId);

	int insertChatAttachment(ChatAttachmentDTO dto);

	//	void insertMessage(@Param("roomId") Long roomId, @Param("senderId") Long senderId, @Param("msgText") String msgText); pk바로 주입하는 기법 사용해서 legacy로 변경.
	//	void insertRoomMember(Long roomId, Long userId);
	//	Long findRoomId(Long user1, Long user2);
}
