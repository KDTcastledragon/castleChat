package com.chat.chatorc.service;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.chat.chatorc.dto.PayloadReadChatMessageResponseDTO;
import com.chat.chatorc.dto.PayloadSendChatMessageRequestDTO;
import com.chat.chatorc.dto.PayloadSendChatMessageResponseDTO;
import com.chat.chatorc.dto.UpdatedUnreadMessagesDTO;
import com.chat.contract.domain.ChatMessagesDTO;

public class ChatService {

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public PayloadSendChatMessageResponseDTO createChatMessage(Long senderUserId, String senderPublicId, PayloadSendChatMessageRequestDTO payload, Set<Long> viewingUserIds) {
		Long roomId = payload.getRoomId();

		// WsHandler에서 검사하긴 했지만, Service에서도 독립적인 방어 필요함.
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (payload.getMessageText() == null) {
			throw new IllegalArgumentException("메시지 내용이 없습니다.");
		}

		// 방 멤버 전체를 Redis에서 가져온다. 없으면 DB에서 가져와 Redis에 올린다.
		Set<Long> totalRoomMemberIds = roomMemberCache.getOrLoadRoomMembers(roomId, () -> chatMapper.findActiveRoomMemberIds(roomId));

		if (totalRoomMemberIds.isEmpty()) {
			throw new IllegalStateException("채팅방 멤버 정보를 찾을 수 없습니다.");
		}

		if (!totalRoomMemberIds.contains(senderUserId)) {
			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
		}

		LocalDateTime now = LocalDateTime.now(); // 서버시간 기준으로 한다. FE에서 time을 조작할 수도 있기 때문이다. 우린 서버를 신뢰한다.
		// DB에서 created_at default current_timestamp로 넣는 구조면, insert 후에 MyBatis가 createdAt까지 자동으로 채워주지는 않아. useGeneratedKeys로 보통 messageId만 들어와.

		ChatMessagesDTO insertChat = new ChatMessagesDTO();
		insertChat.setRoomId(roomId);
		insertChat.setSenderId(senderUserId);
		insertChat.setMessageText(payload.getMessageText());
		insertChat.setCreatedAt(now);
		chatMapper.insertMessage(insertChat); // DB에 Msg 저장.

		//		// --> 카톡도 한번에 urc계산값을 보내지 않는다. 일단 rM수-1만큼 보내고, fe에서 readMsg를 보내서 아주빠르게 urc를 감소시킨다. 그래서, viewing 필요없다.
		//		// 현재 방을 보고 있는 사람들은 메시지를 즉시 받은 상태니까 읽은 사람으로 본다. 보낸 사람도 자기 메시지는 당연히 읽은 상태니까 추가한다.
		//		Set<Long> viewingRoomMemberIds = new HashSet<>(viewingUserIds);
		//		viewingRoomMemberIds.add(senderUserId);
		//
		//		viewingRoomMemberIds.retainAll(totalRoomMemberIds); // 혹시 이상한 userId가 섞였더라도 실제 방 멤버만 남긴다.
		//
		//		log.info("{}방의 총 유저 : {}  , 현재 연결된 유저 : {}", roomId, totalRoomMemberIds, viewingRoomMemberIds);
		//
		//		Long unreadCount = (long) (totalRoomMemberIds.size() - viewingRoomMemberIds.size()); // 안 읽은 사람 수 계산.
		//
		//		log.info("unreadCount : {}", unreadCount);
		//
		//		if (unreadCount < 0) {
		//			unreadCount = 0L; // == (long) 0;
		//		}

		//		근데 왜 urc나 roomMC같은 건 int최대인 21억을 넘지않는데, 굳이 int 안 쓰는 이유는?
		//		네, unreadCount나 roomMemberCount 자체는 현실적으로 int 범위를 넘지 않을 가능성이 큽니다.
		//		다만 프로젝트의 주요 식별자가 BIGINT 기반이고, MyBatis의 COUNT 결과도 Long으로 받는 경우가 많아서 DTO/API 계층에서는 Long으로 통일했습니다.
		//		타입을 섞으면 int/long 변환이 반복되고, 추후 DB 집계값이나 Redis size 결과와도 타입 불일치가 생겨서 Long을 선택했습니다.
		//		DB의 COUNT(*) 결과나 Redis Set size 결과는 Java에서 Long으로 다루는 게 자연스럽고,
		//		room_id/user_id/message_id도 BIGINT라 채팅 도메인에서는 숫자 타입을 Long 중심으로 맞췄습니다.
		//		실제 화면 표시 단계에서는 필요하면 Number/int로 변환할 수 있지만,
		//		서버 내부 DTO에서는 타입 일관성을 우선했습니다.

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

		List<UpdatedUnreadMessagesDTO> updatedChatList = chatMapper.getUpdatedUnreadCountChatMessages(roomId, oldLastReadMsgId, newLastReadMessageId);
		log.info("READ_MSG 계산 결과 roomId={}, readerUserId={}, updatedMessages={}", roomId, readerUserId, updatedChatList);
		return new PayloadReadChatMessageResponseDTO(roomId, readerPublicId, newLastReadMessageId, updatedChatList);
	}

	//
	//	@Override
	//	public void updateLastRead(Long roomId, Long userId, Long lastReadMessageId) {
	//		chatMapper.updateLastRead(roomId, userId, lastReadMessageId);
	//	}
}
