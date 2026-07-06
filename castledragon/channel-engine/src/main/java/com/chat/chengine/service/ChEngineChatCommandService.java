package com.chat.chengine.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chengine.mapper.ChEngineChatMapper;
import com.chat.chengine.usecase.ChEngineChatCommandUseCase;
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
public class ChEngineChatCommandService implements ChEngineChatCommandUseCase {

	//	@Autowired 빼는 이유 : 필수 의존성이 명확함. final 가능. 테스트 쉬움. Spring 권장 방식. 객체 생성 시점에 의존성 누락을 바로 알 수 있음
	private final ChEngineChatMapper chatMapper;

	private final RoomMemberCache roomMemberCache;
	private final RoomReadPositionCache roomReadPositionCache;

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public ChatMessageViewResponseDTO createChatMessage(CreateChatMessageCommand cmd) {
		// 필수 인자 여부 검증.
		if (cmd.getRoomId() == null) {
			log.error("id:{} 채팅의 roomId없음 : {}", cmd.getSenderUserId(), cmd.getRoomId());
			throw new IllegalArgumentException("No RoomId");
		}

		if (cmd.getMessageText() == null) {
			log.error("id:{} 채팅의 msg없음 : {}", cmd.getSenderUserId(), cmd.getMessageText());
			throw new IllegalArgumentException("no msg");
		}

		// 채팅방 내 모든 멤버 불러오기. 현재 sender가 실제 이 채팅방의 멤버인지 검증하기 위함.
		Set<Long> allActiveMemberIdsInRoom = roomMemberCache
				.getOrLoadRoomMembers(cmd.getRoomId(), () -> chatMapper.findAllActiveMemberIdsInRoom(cmd.getRoomId()));

		if (allActiveMemberIdsInRoom.isEmpty()) {
			log.error("id:{}의 채팅방 멤버 정보를 찾을 수 없음. {}", cmd.getSenderUserId(), allActiveMemberIdsInRoom);
			throw new IllegalArgumentException("채팅방 멤버 정보를 찾을 수 없음.");
		}

		if (!allActiveMemberIdsInRoom.contains(cmd.getSenderUserId())) {
			log.error("id:{}는 {}-채팅방의 멤버가 아닙니다. {}", cmd.getSenderUserId(), cmd.getRoomId(), allActiveMemberIdsInRoom);
			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
		}

		LocalDateTime now = LocalDateTime.now();

		ChatMessagesDTO msg = new ChatMessagesDTO();
		msg.setRoomId(cmd.getRoomId());
		msg.setSenderId(cmd.getSenderUserId());
		msg.setMessageText(cmd.getMessageText());
		msg.setCreatedAt(now);

		// DB insert
		int isCreated = chatMapper.insertChatMessage(msg); // useGK + keyP 설정 => msgId PK 사용 가능. 

		if (isCreated != 1) {
			throw new IllegalStateException("insert Msg Failed");
		}

		// ====== sender의 lastReadMsg In Room도 적용시켜준다. 단, readMsg 흐름과 다르게 독립적으로 조용히. ==============================================================
		ReadPositionUpdateResult rslt = roomReadPositionCache.updateIfGreater(cmd.getRoomId(), cmd.getSenderUserId(), msg
				.getMessageId(), () -> chatMapper.findLastReadMessageId(cmd.getRoomId(), cmd.getSenderUserId()));
		log.info("[sendMsg]redisGreater 결과 = room:{} sender:{} old:{} new:{}", cmd.getRoomId(), cmd.getSenderUserId(), rslt
				.oldLastReadMessageId(), rslt.newLastReadMessageId());

		Long unreadCount = Math.max(allActiveMemberIdsInRoom.size() - 1L, 0L); // 메시지 읽지 않은 멤버 수. (sender 제외)

		ChatMessageViewResponseDTO response = new ChatMessageViewResponseDTO();
		response.setMessageId(msg.getMessageId());
		response.setRoomId(msg.getRoomId());
		response.setSenderPublicId(cmd.getSenderPublicId());
		response.setMessageText(msg.getMessageText());
		response.setCreatedAt(msg.getCreatedAt());
		response.setUnreadCount(unreadCount);

		return response;
	}

	@Override
	@Transactional
	public ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand cmd) {
		if (cmd.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (cmd.getReaderUserId() == null) {
			throw new IllegalArgumentException("readerUserId가 없습니다.");
		}

		if (cmd.getReaderPublicId() == null) {
			throw new IllegalArgumentException("readerPublicId가 없습니다.");
		}

		if (cmd.getLastReadMessageId() == null) {
			throw new IllegalArgumentException("lastReadMessageId가 없습니다.");
		}

		log.info("gRPC chengine readService cmd : {}", cmd);

		ReadPositionUpdateResult updateResult = roomReadPositionCache.updateIfGreater(cmd.getRoomId(), cmd.getReaderUserId(), cmd
				.getLastReadMessageId(), () -> chatMapper.findLastReadMessageId(cmd.getRoomId(), cmd.getReaderUserId()));
		log.info("[readMsg]redisGreater 결과 = room:{} reader:{} old:{} new:{}", cmd.getRoomId(), cmd.getReaderUserId(), updateResult
				.oldLastReadMessageId(), updateResult.newLastReadMessageId());

		log.info("gRPC chengine readService updateResult : {}", updateResult);

		return new ReadPositionUpdateResponseDTO(cmd.getRoomId(), cmd.getReaderPublicId(), updateResult.oldLastReadMessageId(), updateResult
				.newLastReadMessageId(), updateResult.updated());
	}

	@Override
	@Transactional
	public DeleteChatMessageResponseDTO deleteChatMessage(DeleteChatMessageCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("삭제 요청이 없습니다.");
		}

		if (cmd.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (cmd.getMessageId() == null) {
			throw new IllegalArgumentException("messageId가 없습니다.");
		}

		if (cmd.getDeleterUserId() == null) {
			throw new IllegalArgumentException("deleterUserId가 없습니다.");
		}

		if (cmd.getDeleterPublicId() == null || cmd.getDeleterPublicId().isBlank()) {
			throw new IllegalArgumentException("deleterPublicId가 없습니다.");
		}

		// room row lock까지는 과하고, message row만 잠그면 됨.
		Long lockedMessageId = chatMapper.lockChatMessageForUpdate(cmd.getRoomId(), cmd.getMessageId());

		if (lockedMessageId == null) {
			throw new IllegalArgumentException("존재하지 않는 메시지입니다.");
		}

		String messageStatus = chatMapper.findChatMessageStatus(cmd.getRoomId(), cmd.getMessageId());

		if ("DELETED".equals(messageStatus)) {
			throw new IllegalStateException("이미 삭제된 메시지입니다.");
		}

		Long senderUserId = chatMapper.findChatMessageSenderUserId(cmd.getRoomId(), cmd.getMessageId());

		if (senderUserId == null) {
			throw new IllegalArgumentException("메시지 작성자 정보를 찾을 수 없습니다.");
		}

		if (!cmd.getDeleterUserId().equals(senderUserId)) {
			throw new IllegalArgumentException("메시지 작성자만 삭제할 수 있습니다.");
		}

		int updated = chatMapper.deleteChatMessage(cmd.getRoomId(), cmd.getMessageId(), cmd.getDeleterUserId());

		if (updated != 1) {
			throw new IllegalStateException("메시지 삭제 실패");
		}

		return new DeleteChatMessageResponseDTO(cmd.getRoomId(), cmd.getMessageId(), cmd.getDeleterPublicId(), "DELETED", LocalDateTime.now());
	}

	@Override
	@Transactional
	public ReactChatMessageEventResponseDTO reactChatMessage(ReactChatMessageCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("리액션 요청이 없습니다.");
		}

		if (cmd.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (cmd.getMessageId() == null) {
			throw new IllegalArgumentException("messageId가 없습니다.");
		}

		if (cmd.getReactorUserId() == null) {
			throw new IllegalArgumentException("reactorUserId가 없습니다.");
		}

		if (cmd.getReactorPublicId() == null || cmd.getReactorPublicId().isBlank()) {
			throw new IllegalArgumentException("reactorPublicId가 없습니다.");
		}

		if (cmd.getReactionType() == null || cmd.getReactionType().isBlank()) {
			throw new IllegalArgumentException("reactionType이 없습니다.");
		}

		if (cmd.getReactionCode() == null || cmd.getReactionCode().isBlank()) {
			throw new IllegalArgumentException("reactionCode가 없습니다.");
		}

		if (cmd.getAddRequested() == null) {
			throw new IllegalArgumentException("addRequested가 없습니다.");
		}

		Long lockedMessageId = chatMapper.lockChatMessageForUpdate(cmd.getRoomId(), cmd.getMessageId());

		if (lockedMessageId == null) {
			throw new IllegalArgumentException("존재하지 않는 메시지입니다.");
		}

		String messageStatus = chatMapper.findChatMessageStatus(cmd.getRoomId(), cmd.getMessageId());

		if ("DELETED".equals(messageStatus)) {
			throw new IllegalStateException("삭제된 메시지에는 리액션할 수 없습니다.");
		}

		Set<Long> allActiveMemberIdsInRoom = roomMemberCache
				.getOrLoadRoomMembers(cmd.getRoomId(), () -> chatMapper.findAllActiveMemberIdsInRoom(cmd.getRoomId()));

		if (allActiveMemberIdsInRoom.isEmpty()) {
			throw new IllegalArgumentException("채팅방 멤버 정보를 찾을 수 없습니다.");
		}

		if (!allActiveMemberIdsInRoom.contains(cmd.getReactorUserId())) {
			throw new IllegalArgumentException("채팅방 멤버만 리액션할 수 있습니다.");
		}

		LocalDateTime now = LocalDateTime.now();

		if (Boolean.TRUE.equals(cmd.getAddRequested())) {
			int inserted = chatMapper.insertChatMessageReaction(cmd);

			if (inserted < 1) {
				throw new IllegalStateException("리액션 추가 실패");
			}

			return new ReactChatMessageEventResponseDTO(cmd.getRoomId(), cmd.getMessageId(), cmd.getReactorPublicId(), cmd.getReactionType(), cmd
					.getReactionCode(), true, now);
		}

		int deleted = chatMapper.deleteChatMessageReaction(cmd.getRoomId(), cmd.getMessageId(), cmd.getReactorUserId(), cmd.getReactionCode());

		if (deleted < 1) {
			throw new IllegalStateException("리액션 취소 실패");
		}

		return new ReactChatMessageEventResponseDTO(cmd.getRoomId(), cmd.getMessageId(), cmd.getReactorPublicId(), cmd.getReactionType(), cmd
				.getReactionCode(), false, now);
	}

}
