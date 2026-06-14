package com.chat.castledragon.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.castledragon.domain.ChatMessagesDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.ChatRoomsDTO;
import com.chat.castledragon.domain.PayloadSendChatMessageResponseDTO;
import com.chat.castledragon.domain.RoomMemberResponseDTO;
import com.chat.castledragon.domain.RoomMembersDTO;
import com.chat.castledragon.domain.UpdatedUnreadMessagesDTO;

@Mapper
public interface ChatMapper {

	Long findRoomId(@Param("user1") Long user1, @Param("user2") Long user2);

	//	void createRoom(@Param("roomType") String roomType, @Param("roomStatus") String roomStatus); //roomId를 받을 parameter가 없다.
	int createRoom(ChatRoomsDTO dto); //DTO내부 getter를 통해 접근 가능해서, 이렇게만 적어도 된다.

	void insertRoomMember(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("role") String role, @Param("customRoomName") String customRoomName, @Param("customRoomThumbnail") String customRoomThumbnail, @Param("memberStatus") String memberStatus);

	List<ChatMessagesDTO> getMessages(Long roomId);

	void insertMessage(ChatMessagesDTO dto);

	void updateLastRead(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("lastReadMessageId") Long lastReadMessageId);

	List<ChatRoomListDTO> getMyAllChatRooms(Long userId);

	List<Long> findActiveRoomMemberIds(Long roomId);

	List<PayloadSendChatMessageResponseDTO> loadMessagesInRoom(@Param("roomId") Long roomId);

	ChatRoomsDTO findDirectRoom(@Param("user1") Long user1, @Param("user2") Long user2);

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
