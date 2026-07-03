package com.chat.domserv.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.domain.ChatRoomListDTO;
import com.chat.contract.domain.ChatRoomsDTO;
import com.chat.contract.domain.RoomMemberResponseDTO;
import com.chat.contract.domain.RoomMembersDTO;

@Mapper
public interface RoomMapper {

	Long findRoomId(@Param("user1") Long user1, @Param("user2") Long user2);

	//	void createRoom(@Param("roomType") String roomType, @Param("roomStatus") String roomStatus); //roomId를 받을 parameter가 없다.
	int createRoom(ChatRoomsDTO dto); //DTO내부 getter를 통해 접근 가능해서, 이렇게만 적어도 된다.

	int insertRoomMember(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("role") String role, @Param("customRoomName") String customRoomName, @Param("customRoomThumbnail") String customRoomThumbnail, @Param("memberStatus") String memberStatus);

	List<ChatRoomListDTO> getMyAllChatRooms(Long userId);

	ChatRoomsDTO findDirectRoom(@Param("user1") Long user1, @Param("user2") Long user2);

	ChatRoomsDTO getRoomByRoomId(Long roomId);

	RoomMembersDTO getActiveRoomMemberInfoInRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

	List<RoomMemberResponseDTO> getRoomMemberProfilesByRoomId(Long roomId);

	int leftRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

	int kickMemberInRoom(@Param("roomId") Long roomId, @Param("kickerUserId") Long kickerUserId, @Param("kickedUserId") Long kickedUserId);

	void banMemberInRoom(@Param("roomId") Long roomId, @Param("bannerUserId") Long bannerUserId, @Param("bannedUserId") Long bannedUserId);

	String findRoleInRoomByUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

	int reactivateRoomMember(Long roomId, List<Long> directMemberPublicIds);
}

// Mapper는 SQL 소유권 기준으로 RoomMapper, MessageMapper, UserMapper처럼 도메인별로 유지했다.
// Mapper까지 Command/Query로 쪼개면 SQL 탐색 비용과 파일 수가 늘어나는 오버엔지니어링이라 판단했다.
// Mapper는 “비즈니스 계약”이 아니라 “DB 접근 세부 구현”에 가까워.
// Service usecase처럼 Controller가 의존하는 public contract가 아님.
// 결론 : Service 계층은 Controller가 의존하는 application contract라서 Command/Query로 분리했습니다. 
// 		반면 Mapper는 MyBatis SQL 접근 계층이기 때문에 무조건 Command/Query로 나누지 않았습니다. 
// 		현재는 RoomMapper, MessageMapper처럼 도메인/테이블 책임 기준으로 나누고, 조회 부하가 커지거나 read/write datasource가 분리되는 시점에 ReadMapper를 별도로 분리할 계획입니다. 
//		지금 단계에서 Mapper까지 CQRS로 쪼개는 건 오버엔지니어링이라고 판단했습니다.