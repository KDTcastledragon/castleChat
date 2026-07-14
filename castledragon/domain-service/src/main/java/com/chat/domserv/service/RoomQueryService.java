package com.chat.domserv.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.contract.room.domain.ChatRoomListDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewDTO;
import com.chat.domserv.mapper.RoomMapper;
import com.chat.domserv.usecase.RoomQueryUseCase;
import com.chat.redis.cache.RoomReadPositionCache;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RoomQueryService implements RoomQueryUseCase {

	@Autowired
	RoomMapper roomMapper;

	@Autowired
	RoomReadPositionCache roomReadPositionCache;

	@Override
	public List<ChatRoomListDTO> getMyAllChatRooms(Long userId) {
		List<ChatRoomListDTO> roomList = roomMapper.getMyAllChatRooms(userId);

		// 읽음 위치는 Redis가 최신이다(DB는 dirty flush 전까지 과거값).
		// DB 기준으로 계산된 unread를 Redis LRM으로 보정해야 읽은 방이 다시 unread로 뜨지 않는다.
		for (ChatRoomListDTO room : roomList) {
			if (room.getUnreadCount() == null || room.getUnreadCount() == 0) {
				continue;
			}

			Long cachedLrm = roomReadPositionCache.getLastReadMessageId(room.getRoomId(), userId);
			Long dbLrm = room.getMyLastReadMessageId() == null ? 0L : room.getMyLastReadMessageId();

			if (cachedLrm != null && cachedLrm > dbLrm) {
				room.setUnreadCount(roomMapper.countUnreadMessages(room.getRoomId(), userId, cachedLrm));
			}
		}

		return roomList;
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomNoticeViewDTO> loadRoomNotices(Long roomId, Long beforeRoomNoticeId, int limit, Long requesterUserId) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (requesterUserId == null || roomMapper.countActiveRoomMember(roomId, requesterUserId) < 1) {
			throw new IllegalArgumentException("현재 채팅방의 멤버가 아닙니다.");
		}

		int pageSize = limit <= 0 ? 20 : Math.min(limit, 20);

		return roomMapper.findRoomNotices(roomId, beforeRoomNoticeId, pageSize);
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
