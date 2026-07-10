package com.chat.domserv.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.contract.chatting.domain.ChatAttachmentDTO;
import com.chat.contract.chatting.domain.res.ChatMessageReactionMemberResponseDTO;
import com.chat.contract.chatting.domain.res.ChatMessageReactionSummaryDTO;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.redis.RedisRoomMemberReadPositionDTO;
import com.chat.contract.room.domain.res.RoomMemberResponseDTO;
import com.chat.domserv.domain.MessageReaderCandidateDTO;
import com.chat.domserv.mapper.DomServChatMapper;
import com.chat.domserv.usecase.ChatQueryUseCase;
import com.chat.redis.cache.RoomReadPositionCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChatQueryService implements ChatQueryUseCase {
	private final DomServChatMapper domServChatMapper;

	private final RoomReadPositionCache roomReadPositionCache;

	private void warmUpRoomReadPositions(Long roomId) {
		List<RedisRoomMemberReadPositionDTO> members = domServChatMapper.findActiveRoomReadPositions(roomId);

		if (members == null || members.isEmpty()) {
			return;
		}

		for (RedisRoomMemberReadPositionDTO member : members) {
			Long dbLastReadMessageId = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();
			Long visibleAfterMessageId = member.getVisibleAfterMessageId() == null ? 0L : member.getVisibleAfterMessageId();

			Long warmUpLastReadMessageId = Math.max(dbLastReadMessageId, visibleAfterMessageId);

			boolean warmed = roomReadPositionCache.warmUpIfGreater(roomId, member.getUserId(), warmUpLastReadMessageId);

			if (warmed) {
				log.info("room read-position warm-up. roomId={}, userId={}, lrm={}", roomId, member.getUserId(), warmUpLastReadMessageId);
			}
		}
	}

	private Long getEffectiveLastReadMessageId(Long roomId, RedisRoomMemberReadPositionDTO member) {
		Long dbLastReadMessageId = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();
		Long visibleAfterMessageId = member.getVisibleAfterMessageId() == null ? 0L : member.getVisibleAfterMessageId();
		Long cachedLastReadMessageId = roomReadPositionCache.getLastReadMessageId(roomId, member.getUserId());

		Long effectiveLastReadMessageId = Math.max(dbLastReadMessageId, visibleAfterMessageId);

		if (cachedLastReadMessageId != null) {
			effectiveLastReadMessageId = Math.max(effectiveLastReadMessageId, cachedLastReadMessageId);
		}

		return effectiveLastReadMessageId;
	}

	private void applyRealtimeUnreadCounts(Long roomId, List<ChatMessageViewResponseDTO> chatList) {
		List<RedisRoomMemberReadPositionDTO> members = domServChatMapper.findActiveRoomReadPositions(roomId);

		if (members == null || members.isEmpty()) {
			return;
		}

		for (ChatMessageViewResponseDTO message : chatList) {
			Long messageId = message.getMessageId();

			if (messageId == null) {
				continue;
			}

			long unreadCount = members.stream()
					.filter(member -> !Objects.equals(member.getPublicId(), message.getSenderPublicId()))
					.filter(member -> getEffectiveLastReadMessageId(roomId, member) < messageId)
					.count();

			message.setUnreadCount(unreadCount);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatMessageViewResponseDTO> loadMessagesInRoom(Long roomId, Long beforeMessageId, int limit, Long requesterUserId) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (limit <= 0) {
			limit = 50;
		} else if (limit > 100) {
			limit = 100;
		}

		List<ChatMessageViewResponseDTO> chatList = domServChatMapper.loadMessagesInRoom(roomId, beforeMessageId, limit);

		if (chatList != null && !chatList.isEmpty()) {
			List<Long> messageIds = chatList.stream()
					.map(ChatMessageViewResponseDTO::getMessageId)
					.toList();

			List<ChatAttachmentDTO> attachments = domServChatMapper.findChatAttachmentsByMessageIds(messageIds);

			Map<Long, List<ChatAttachmentDTO>> attachmentMap = (attachments == null ? List.<ChatAttachmentDTO>of() : attachments)
					.stream()
					.collect(Collectors.groupingBy(ChatAttachmentDTO::getMessageId));

			List<ChatMessageReactionSummaryDTO> reactionSummaries = domServChatMapper.findReactionSummariesByMessageIds(messageIds, requesterUserId);

			Map<Long, List<ChatMessageReactionSummaryDTO>> reactionMap = (reactionSummaries == null ? List.<ChatMessageReactionSummaryDTO>of() : reactionSummaries)
					.stream()
					.collect(Collectors.groupingBy(ChatMessageReactionSummaryDTO::getMessageId));

			chatList.forEach(message -> {
				message.setAttachments(attachmentMap.getOrDefault(message.getMessageId(), List.of()));
				message.setReactions(reactionMap.getOrDefault(message.getMessageId(), List.of()));
			});

			applyRealtimeUnreadCounts(roomId, chatList);
		}

		warmUpRoomReadPositions(roomId);

		return chatList;
	}

	private void validateActiveRoomMember(Long roomId, Long requesterUserId) {
		if (requesterUserId == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		int count = domServChatMapper.countActiveRoomMember(roomId, requesterUserId);

		if (count < 1) {
			throw new IllegalArgumentException("현재 채팅방의 멤버가 아닙니다.");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatMessageReactionMemberResponseDTO> findMessageReactionMembers(Long roomId, Long messageId, Long requesterUserId) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (messageId == null) {
			throw new IllegalArgumentException("messageId가 없습니다.");
		}

		validateActiveRoomMember(roomId, requesterUserId);

		return domServChatMapper.findMessageReactionMembers(roomId, messageId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomMemberResponseDTO> findMessageReaders(Long roomId, Long messageId, Long requesterUserId) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (messageId == null) {
			throw new IllegalArgumentException("messageId가 없습니다.");
		}

		validateActiveRoomMember(roomId, requesterUserId);

		List<MessageReaderCandidateDTO> candidates = domServChatMapper.findMessageReaderCandidates(roomId, messageId);

		return (candidates == null ? List.<MessageReaderCandidateDTO>of() : candidates)
				.stream()
				.filter(candidate -> {
					Long cachedLastReadMessageId = roomReadPositionCache.getLastReadMessageId(roomId, candidate.getUserId());
					Long dbLastReadMessageId = candidate.getLastReadMessageId() == null ? 0L : candidate.getLastReadMessageId();
					Long effectiveLastReadMessageId = cachedLastReadMessageId == null ? dbLastReadMessageId : Math.max(cachedLastReadMessageId, dbLastReadMessageId);

					return effectiveLastReadMessageId >= messageId;
				})
				.map(candidate -> new RoomMemberResponseDTO(candidate.getPublicId(), candidate.getNickname(), candidate
						.getFriendCode(), candidate.getProfileImg(), candidate.getRole()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, Long> findMessageUnreadCounts(Long roomId, List<Long> messageIds, Long requesterUserId) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (messageIds == null || messageIds.isEmpty()) {
			return Map.of();
		}

		validateActiveRoomMember(roomId, requesterUserId);

		List<Long> distinctMessageIds = messageIds.stream()
				.filter(Objects::nonNull)
				.distinct()
				.limit(300)
				.toList();

		if (distinctMessageIds.isEmpty()) {
			return Map.of();
		}

		List<ChatMessageViewResponseDTO> messages = domServChatMapper.findMessageSenderPublicIds(roomId, distinctMessageIds);
		List<RedisRoomMemberReadPositionDTO> members = domServChatMapper.findActiveRoomReadPositions(roomId);
		List<RedisRoomMemberReadPositionDTO> activeMembers = members == null ? List.of() : members;

		return (messages == null ? List.<ChatMessageViewResponseDTO>of() : messages)
				.stream()
				.collect(Collectors.toMap(ChatMessageViewResponseDTO::getMessageId, message -> activeMembers
						.stream()
						.filter(member -> !Objects.equals(member.getPublicId(), message.getSenderPublicId()))
						.filter(member -> getEffectiveLastReadMessageId(roomId, member) < message.getMessageId())
						.count(), (left, right) -> left));
	}

	//	@Override
	//	@Transactional
	//	public List<ChatMessageViewDTO> loadMessagesInRoom_legacy(Long roomId) {
	//
	//		List<ChatMessageViewDTO> chatList = chatMapper.loadMessagesInRoom(roomId);
	//		return chatList;
	//	}
}
