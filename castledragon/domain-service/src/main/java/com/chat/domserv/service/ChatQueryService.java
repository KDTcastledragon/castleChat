package com.chat.domserv.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.contract.cache.RoomMemberReadPositionDTO;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.domserv.mapper.ChatMapper;
import com.chat.domserv.usecase.ChatQueryUseCase;
import com.chat.redis.cache.RoomReadPositionCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChatQueryService implements ChatQueryUseCase {
	private final ChatMapper chatMapper;

	private final RoomReadPositionCache roomReadPositionCache;

	private void warmUpRoomReadPositions(Long roomId) {
		List<RoomMemberReadPositionDTO> members = chatMapper.findActiveRoomReadPositions(roomId);

		if (members == null || members.isEmpty()) {
			return;
		}

		for (RoomMemberReadPositionDTO member : members) {
			Long dbLastReadMessageId = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();
			Long visibleAfterMessageId = member.getVisibleAfterMessageId() == null ? 0L : member.getVisibleAfterMessageId();

			Long warmUpLastReadMessageId = Math.max(dbLastReadMessageId, visibleAfterMessageId);

			boolean warmed = roomReadPositionCache.warmUpIfGreater(roomId, member.getUserId(), warmUpLastReadMessageId);

			if (warmed) {
				log.info("room read-position warm-up. roomId={}, userId={}, lrm={}", roomId, member.getUserId(), warmUpLastReadMessageId);
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatMessageViewDTO> loadMessagesInRoom(Long roomId, Long beforeMessageId, int limit) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (limit <= 0 || limit > 100) {
			limit = 50;
		}

		List<ChatMessageViewDTO> chatList = chatMapper.loadMessagesInRoom(roomId, beforeMessageId, limit);

		warmUpRoomReadPositions(roomId);

		return chatList;
	}

	//	@Override
	//	@Transactional
	//	public List<ChatMessageViewDTO> loadMessagesInRoom_legacy(Long roomId) {
	//
	//		List<ChatMessageViewDTO> chatList = chatMapper.loadMessagesInRoom(roomId);
	//		return chatList;
	//	}
}
