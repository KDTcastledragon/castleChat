package com.chat.chatorc.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chatorc.dto.PayloadReadChatMessageResponseDTO;
import com.chat.chatorc.dto.PayloadSendChatMessageResponseDTO;
import com.chat.chatorc.dto.UpdatedUnreadMessagesDTO;
import com.chat.chatorc.usecase.ChatOrcCommandUseCase;
import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ChatMessagesDTO;
import com.chat.redis.cache.RoomMemberCache;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ChatService implements ChatOrcCommandUseCase {

	RoomMemberCache roomMemberCache;

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public ChatMessageViewDTO createChatMessage(CreateChatMessageCommand command) {

		// WsHandler에서 검사하긴 했지만, Service에서도 독립적인 방어 필요함.
		if (command.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (command.getMessageText() == null) {
			throw new IllegalArgumentException("메시지 내용이 없습니다.");
		}

		// 방 멤버 전체를 Redis에서 가져온다. 없으면 DB에서 가져와 Redis에 올린다.
		Set<Long> totalRoomMemberIds = roomMemberCache.getOrLoadRoomMembers(command.getRoomId(), () -> chatMapper.findActiveRoomMemberIds(roomId));

		if (totalRoomMemberIds.isEmpty()) {
			throw new IllegalStateException("채팅방 멤버 정보를 찾을 수 없습니다.");
		}

		if (!totalRoomMemberIds.contains(command.getSenderUserId())) {
			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
		}

		LocalDateTime now = LocalDateTime.now(); // 서버시간 기준으로 한다. FE에서 time을 조작할 수도 있기 때문이다. 우린 서버를 신뢰한다.
		// DB에서 created_at default current_timestamp로 넣는 구조면, insert 후에 MyBatis가 createdAt까지 자동으로 채워주지는 않아. useGeneratedKeys로 보통 messageId만 들어와.

		ChatMessagesDTO insertChat = new ChatMessagesDTO();
		insertChat.setRoomId(command.getRoomId());
		insertChat.setSenderId(command.getSenderUserId());
		insertChat.setMessageText(command.getMessageText());
		insertChat.setCreatedAt(now);

		chatMapper.insertMessage(insertChat); // DB에 Msg 저장.

		Long unreadCount = Math.max(totalRoomMemberIds.size() - 1L, 0L);

		PayloadSendChatMessageResponseDTO resChat = new PayloadSendChatMessageResponseDTO();
		resChat.setMessageId(insertChat.getMessageId());
		resChat.setRoomId(insertChat.getRoomId());
		resChat.setSenderPublicId(senderPublicId);
		resChat.setMessageText(insertChat.getMessageText());
		resChat.setCreatedAt(insertChat.getCreatedAt());
		resChat.setUnreadCount(unreadCount);

		//		log.info("chatServ -> wsHandler 채팅data 이동 : {}", resChat);

		return resChat;
	}// sendMsg

	@Override
	@Transactional
	public PayloadReadChatMessageResponseDTO readChatMessage(Long roomId, Long readerUserId, String readerPublicId, Long newLastReadMessageId) {

		chatMapper.lockRoomForUpdate(roomId); // last_read update + urc 계산이 동시에 꼬이지 않게, 메서드 시작 시 room row lock을 잡는다.

		Long oldLastReadMsgId = chatMapper.findLastReadMessageId(roomId, readerUserId);

		if (oldLastReadMsgId != null && oldLastReadMsgId >= newLastReadMessageId) {
			PayloadReadChatMessageResponseDTO resDTO = new PayloadReadChatMessageResponseDTO(roomId, readerPublicId, oldLastReadMsgId, List.of());
			return resDTO;
		}

		chatMapper.updateLastRead(roomId, readerUserId, newLastReadMessageId);

		log.info("READ_MSG 계산 시작 roomId={}, readerUserId={}, oldLast={}, newLast={}", roomId, readerUserId, oldLastReadMsgId, newLastReadMessageId);

		List<UpdatedUnreadMessagesDTO> updatedChatList = chatMapper
				.getUpdatedUnreadCountChatMessages(roomId, oldLastReadMsgId, newLastReadMessageId);
		log.info("READ_MSG 계산 결과 roomId={}, readerUserId={}, updatedMessages={}", roomId, readerUserId, updatedChatList);
		return new PayloadReadChatMessageResponseDTO(roomId, readerPublicId, newLastReadMessageId, updatedChatList);
	}

	//
	//	@Override
	//	public void updateLastRead(Long roomId, Long userId, Long lastReadMessageId) {
	//		chatMapper.updateLastRead(roomId, userId, lastReadMessageId);
	//	}
}
