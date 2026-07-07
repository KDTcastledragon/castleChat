package com.chat.chengine.service;

import java.time.LocalDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chengine.mapper.FriendMapper;
import com.chat.chengine.usecase.FriendCommandUseCase;
import com.chat.contract.friend.command.AddFriendCommand;
import com.chat.contract.friend.command.RespondFriendCommand;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class FriendCommandService implements FriendCommandUseCase {

	private static final String ACCEPT = "ACCEPT";
	private static final String REJECT = "REJECT";

	private static final String PENDING = "PENDING";
	private static final String ACCEPTED = "ACCEPTED";
	private static final String REJECTED = "REJECTED";

	private final FriendMapper friendMapper;

	@Override
	@Transactional
	public FriendEventResponseDTO addFriend(AddFriendCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("친구 요청이 없습니다.");
		}

		if (cmd.getRequesterUserId() == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		if (!hasText(cmd.getRequesterPublicId())) {
			throw new IllegalArgumentException("requesterPublicId가 없습니다.");
		}

		if (!hasText(cmd.getTargetPublicId())) {
			throw new IllegalArgumentException("targetPublicId가 없습니다.");
		}

		Long targetUserId = friendMapper.findUserIdByPublicId(cmd.getTargetPublicId());

		if (targetUserId == null) {
			throw new IllegalArgumentException("존재하지 않는 유저입니다.");
		}

		if (cmd.getRequesterUserId().equals(targetUserId)) {
			throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
		}

		try {
			int inserted = friendMapper.addFriend(cmd.getRequesterUserId(), targetUserId);

			if (inserted != 1) {
				throw new IllegalStateException("친구 요청 실패");
			}

		} catch (DuplicateKeyException e) {
			log.info("이미 친구이거나 친구 요청 존재. requesterUserId={}, targetUserId={}", cmd.getRequesterUserId(), targetUserId);
			throw e;
		}

		return new FriendEventResponseDTO("FRIEND_REQUESTED", cmd.getRequesterUserId(), cmd.getRequesterPublicId(), targetUserId, cmd
				.getTargetPublicId(), PENDING, LocalDateTime.now());
	}

	@Override
	@Transactional
	public FriendEventResponseDTO respondFriend(RespondFriendCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("친구 응답 요청이 없습니다.");
		}

		if (cmd.getResponderUserId() == null) {
			throw new IllegalArgumentException("responderUserId가 없습니다.");
		}

		if (!hasText(cmd.getResponderPublicId())) {
			throw new IllegalArgumentException("responderPublicId가 없습니다.");
		}

		if (!hasText(cmd.getRequesterPublicId())) {
			throw new IllegalArgumentException("requesterPublicId가 없습니다.");
		}

		if (!hasText(cmd.getFriendAction())) {
			throw new IllegalArgumentException("friendAction이 없습니다.");
		}

		Long requesterUserId = friendMapper.findUserIdByPublicId(cmd.getRequesterPublicId());

		if (requesterUserId == null) {
			throw new IllegalArgumentException("친구 요청자를 찾을 수 없습니다.");
		}

		if (cmd.getResponderUserId().equals(requesterUserId)) {
			throw new IllegalArgumentException("자기 자신의 친구 요청에는 응답할 수 없습니다.");
		}

		String nextStatus = switch (cmd.getFriendAction()) {
		case ACCEPT -> ACCEPTED;
		case REJECT -> REJECTED;
		default -> throw new IllegalArgumentException("지원하지 않는 친구 응답입니다.");
		};

		int updated = friendMapper.respondFriendRequest(cmd.getResponderUserId(), requesterUserId, nextStatus);

		if (updated != 1) {
			throw new IllegalStateException("친구 요청 응답 실패");
		}

		String eventType = ACCEPTED.equals(nextStatus) ? "FRIEND_ACCEPTED" : "FRIEND_REJECTED";

		return new FriendEventResponseDTO(eventType, requesterUserId, cmd.getRequesterPublicId(), cmd.getResponderUserId(), cmd
				.getResponderPublicId(), nextStatus, LocalDateTime.now());
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}