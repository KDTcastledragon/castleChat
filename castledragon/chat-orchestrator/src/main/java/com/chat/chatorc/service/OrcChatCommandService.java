package com.chat.chatorc.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chatorc.mapper.OrcChatMapper;
import com.chat.chatorc.usecase.OrcChatCommandUseCase;
import com.chat.contract.command.chatting.CreateChatMessageCommand;
import com.chat.contract.command.chatting.DeleteChatMessageCommand;
import com.chat.contract.command.chatting.ReactChatMessageCommand;
import com.chat.contract.command.chatting.ReadChatMessageCommand;
import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.ChatMessagesDTO;
import com.chat.contract.domain.chatting.DeleteChatMessageResponseDTO;
import com.chat.contract.domain.chatting.ReactChatMessageEventResponseDTO;
import com.chat.contract.domain.chatting.ReadPositionUpdateResponseDTO;
import com.chat.redis.cache.ReadPositionUpdateResult;
import com.chat.redis.cache.RoomMemberCache;
import com.chat.redis.cache.RoomReadPositionCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrcChatCommandService implements OrcChatCommandUseCase {

	//	@Autowired 빼는 이유 : 필수 의존성이 명확함. final 가능. 테스트 쉬움. Spring 권장 방식. 객체 생성 시점에 의존성 누락을 바로 알 수 있음
	private final OrcChatMapper chatMapper;

	private final RoomMemberCache roomMemberCache;
	private final RoomReadPositionCache roomReadPositionCache;

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public ChatMessageViewResponseDTO createChatMessage(CreateChatMessageCommand command) {
		// 필수 인자 여부 검증.
		if (command.getRoomId() == null) {
			log.error("id:{} 채팅의 roomId없음 : {}", command.getSenderUserId(), command.getRoomId());
			throw new IllegalArgumentException("No RoomId");
		}

		if (command.getMessageText() == null) {
			log.error("id:{} 채팅의 msg없음 : {}", command.getSenderUserId(), command.getMessageText());
			throw new IllegalArgumentException("no msg");
		}

		// 채팅방 내 모든 멤버 불러오기. 현재 sender가 실제 이 채팅방의 멤버인지 검증하기 위함.
		Set<Long> allActiveMemberIdsInRoom = roomMemberCache
				.getOrLoadRoomMembers(command.getRoomId(), () -> chatMapper.findAllActiveMemberIdsInRoom(command.getRoomId()));

		if (allActiveMemberIdsInRoom.isEmpty()) {
			log.error("id:{}의 채팅방 멤버 정보를 찾을 수 없음. {}", command.getSenderUserId(), allActiveMemberIdsInRoom);
			throw new IllegalArgumentException("채팅방 멤버 정보를 찾을 수 없음.");
		}

		if (!allActiveMemberIdsInRoom.contains(command.getSenderUserId())) {
			log.error("id:{}는 {}-채팅방의 멤버가 아닙니다. {}", command.getSenderUserId(), command.getRoomId(), allActiveMemberIdsInRoom);
			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
		}

		LocalDateTime now = LocalDateTime.now();

		ChatMessagesDTO msg = new ChatMessagesDTO();
		msg.setRoomId(command.getRoomId());
		msg.setSenderId(command.getSenderUserId());
		msg.setMessageText(command.getMessageText());
		msg.setCreatedAt(now);

		// DB insert
		int isCreated = chatMapper.insertChatMessage(msg); // useGK + keyP 설정 => msgId PK 사용 가능. 

		if (isCreated != 1) {
			throw new IllegalStateException("insert Msg Failed");
		}

		// ====== sender의 lastReadMsg In Room도 적용시켜준다. 단, readMsg 흐름과 다르게 독립적으로 조용히. ==============================================================
		ReadPositionUpdateResult rslt = roomReadPositionCache.updateIfGreater(command.getRoomId(), command.getSenderUserId(), msg
				.getMessageId(), () -> chatMapper.findLastReadMessageId(command.getRoomId(), command.getSenderUserId()));
		log.info("[sendMsg]redisGreater 결과 = room:{} sender:{} old:{} new:{}", command.getRoomId(), command.getSenderUserId(), rslt
				.oldLastReadMessageId(), rslt.newLastReadMessageId());

		Long unreadCount = Math.max(allActiveMemberIdsInRoom.size() - 1L, 0L); // 메시지 읽지 않은 멤버 수. (sender 제외)

		ChatMessageViewResponseDTO response = new ChatMessageViewResponseDTO();
		response.setMessageId(msg.getMessageId());
		response.setRoomId(msg.getRoomId());
		response.setSenderPublicId(command.getSenderPublicId());
		response.setMessageText(msg.getMessageText());
		response.setCreatedAt(msg.getCreatedAt());
		response.setUnreadCount(unreadCount);

		return response;
	}

	@Override
	@Transactional
	public ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand command) {
		if (command.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (command.getReaderUserId() == null) {
			throw new IllegalArgumentException("readerUserId가 없습니다.");
		}

		if (command.getReaderPublicId() == null) {
			throw new IllegalArgumentException("readerPublicId가 없습니다.");
		}

		if (command.getLastReadMessageId() == null) {
			throw new IllegalArgumentException("lastReadMessageId가 없습니다.");
		}

		log.info("gRPC chatOrc readService cmd : {}", command);

		ReadPositionUpdateResult updateResult = roomReadPositionCache.updateIfGreater(command.getRoomId(), command.getReaderUserId(), command
				.getLastReadMessageId(), () -> chatMapper.findLastReadMessageId(command.getRoomId(), command.getReaderUserId()));
		log.info("[readMsg]redisGreater 결과 = room:{} reader:{} old:{} new:{}", command.getRoomId(), command.getReaderUserId(), updateResult
				.oldLastReadMessageId(), updateResult.newLastReadMessageId());

		log.info("gRPC chatOrc readService updateResult : {}", updateResult);

		return new ReadPositionUpdateResponseDTO(command.getRoomId(), command.getReaderPublicId(), updateResult
				.oldLastReadMessageId(), updateResult.newLastReadMessageId(), updateResult.updated());
	}

	@Override
	public DeleteChatMessageResponseDTO deleteChatMessage(DeleteChatMessageCommand command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReactChatMessageEventResponseDTO reactChatMessage(ReactChatMessageCommand command) {
		// TODO Auto-generated method stub
		return null;
	}

}
