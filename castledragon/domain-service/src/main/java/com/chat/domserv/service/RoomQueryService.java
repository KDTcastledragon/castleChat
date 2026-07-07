package com.chat.domserv.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chat.contract.room.domain.ChatRoomListDTO;
import com.chat.domserv.mapper.RoomMapper;
import com.chat.domserv.usecase.RoomQueryUseCase;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RoomQueryService implements RoomQueryUseCase {

	@Autowired
	RoomMapper roomMapper;

	@Override
	public List<ChatRoomListDTO> getMyAllChatRooms(Long userId) {
		List<ChatRoomListDTO> roomList = roomMapper.getMyAllChatRooms(userId);
		return roomList;
	}

	//	@Override
	//	public EnterRoomResponseDTO enterExistedRoom(Long roomId, SessionUserDTO me) {
	//		ChatRoomsDTO room = roomMapper.getRoomByRoomId(roomId);
	//		if (room == null) {
	//			throw new IllegalArgumentException("존재하지 않는 채팅방입니다.");
	//		}
	//
	//		RoomMembersDTO myInfo = roomMapper.getActiveRoomMemberInfoInRoom(roomId, me.getUserId());
	//
	//		if (myInfo == null) {
	//			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
	//		}
	//
	//		List<RoomMemberResponseDTO> memberList = roomMapper.getRoomMemberProfilesByRoomId(roomId);
	//
	//		if (memberList == null || memberList.isEmpty()) {
	//			throw new IllegalStateException("채팅방 멤버 정보를 찾을 수 없습니다.");
	//		}
	//
	//		EnterRoomResponseDTO resdto = new EnterRoomResponseDTO(roomId, room.getRoomType(), myInfo.getCustomRoomName(), myInfo
	//				.getCustomRoomThumbnail(), null, (long) memberList.size(), memberList, null);
	//
	//		return resdto;
	//	}

}
