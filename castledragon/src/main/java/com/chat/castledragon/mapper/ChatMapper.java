package com.chat.castledragon.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.castledragon.domain.ChatDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.ChatRoomsDTO;

@Mapper
public interface ChatMapper {

	List<ChatDTO> getListWithFri(String userId, String friId);

	Long findRoomId(@Param("user1") Long user1, @Param("user2") Long user2);

	void createRoom(ChatRoomsDTO room); //DTO내부 getter를 통해 접근 가능해서, 이렇게만 적어도 된다.

	void insertRoomMember(@Param("roomId") Long roomId, @Param("userId") Long userId);

	List<ChatDTO> getMessages(Long roomId);

	void insertMessage(ChatDTO dto);

	void updateLastRead(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("lastReadMessageId") Long lastReadMessageId);

	List<ChatRoomListDTO> getMyChatRooms(Long userId);

	List<Long> findActiveRoomMemberIds(Long roomId);

	//	void insertMessage(@Param("roomId") Long roomId, @Param("senderId") Long senderId, @Param("msgText") String msgText); pk바로 주입하는 기법 사용해서 legacy로 변경.
	//	void insertRoomMember(Long roomId, Long userId);
	//	Long findRoomId(Long user1, Long user2);
}
