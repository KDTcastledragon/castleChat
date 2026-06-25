package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ChatMessagesDTO;
import com.chat.contract.domain.ChatRoomsDTO;
import com.chat.contract.domain.RoomMemberResponseDTO;
import com.chat.contract.domain.RoomMembersDTO;

@Mapper
public interface ChatMapper {

	List<ChatMessagesDTO> getMessages(Long roomId);

	void insertMessage(ChatMessagesDTO dto);

	void updateLastRead(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("lastReadMessageId") Long lastReadMessageId);

	List<Long> findActiveRoomMemberIds(Long roomId);

	List<ChatMessageViewDTO> loadMessagesInRoom(@Param("roomId") Long roomId);

	Long findLastReadMessageId(@Param("roomId") Long roomId, @Param("userId") Long userId);

	List<UpdatedUnreadMessagesDTO> getUpdatedUnreadCountChatMessages(@Param("roomId") Long roomId, @Param("oldLastReadMsgId") Long oldLastReadMsgId, @Param("newLastReadMessageId") Long newLastReadMessageId);

	ChatRoomsDTO getRoomByRoomId(Long roomId);

	RoomMembersDTO getMyInfoFromRoomMembers(@Param("roomId") Long roomId, @Param("userId") Long userId);

	List<RoomMemberResponseDTO> getRoomMemberProfilesByRoomId(Long roomId);

	Long lockRoomForUpdate(@Param("roomId") Long roomId);

	void leftRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

	void decreaseActiveMemberCount(@Param("roomId") Long roomId);

	void reactivateRoomMember(@Param("roomId") Long roomId, @Param("userId") Long userId);
	//	void insertMessage(@Param("roomId") Long roomId, @Param("senderId") Long senderId, @Param("msgText") String msgText); pk바로 주입하는 기법 사용해서 legacy로 변경.
	//	void insertRoomMember(Long roomId, Long userId);
	//	Long findRoomId(Long user1, Long user2);
}
