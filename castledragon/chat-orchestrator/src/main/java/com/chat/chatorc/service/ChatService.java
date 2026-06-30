package com.chat.chatorc.service;

import org.springframework.stereotype.Service;

import com.chat.redis.cache.RoomMemberCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChatService {

	RoomMemberCache roomMemberCache;

	//	@Override
	//	@Transactional
	//	public PayloadReadChatMessageResponseDTO readChatMessage(Long roomId, Long readerUserId, String readerPublicId, Long newLastReadMessageId) {
	//
	//		chatMapper.lockRoomForUpdate(roomId); // last_read update + urc 계산이 동시에 꼬이지 않게, 메서드 시작 시 room row lock을 잡는다.
	//
	//		Long oldLastReadMsgId = chatMapper.findLastReadMessageId(roomId, readerUserId);
	//
	//		if (oldLastReadMsgId != null && oldLastReadMsgId >= newLastReadMessageId) {
	//			PayloadReadChatMessageResponseDTO resDTO = new PayloadReadChatMessageResponseDTO(roomId, readerPublicId, oldLastReadMsgId, List.of());
	//			return resDTO;
	//		}
	//
	//		chatMapper.updateLastRead(roomId, readerUserId, newLastReadMessageId);
	//
	//		log.info("READ_MSG 계산 시작 roomId={}, readerUserId={}, oldLast={}, newLast={}", roomId, readerUserId, oldLastReadMsgId, newLastReadMessageId);
	//
	//		List<UpdatedUnreadMessagesDTO> updatedChatList = chatMapper
	//				.getUpdatedUnreadCountChatMessages(roomId, oldLastReadMsgId, newLastReadMessageId);
	//		log.info("READ_MSG 계산 결과 roomId={}, readerUserId={}, updatedMessages={}", roomId, readerUserId, updatedChatList);
	//		return new PayloadReadChatMessageResponseDTO(roomId, readerPublicId, newLastReadMessageId, updatedChatList);
	//	}

	//
	//	@Override
	//	public void updateLastRead(Long roomId, Long userId, Long lastReadMessageId) {
	//		chatMapper.updateLastRead(roomId, userId, lastReadMessageId);
	//	}
}
