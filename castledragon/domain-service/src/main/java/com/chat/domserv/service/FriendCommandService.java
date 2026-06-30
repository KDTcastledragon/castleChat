package com.chat.domserv.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.chat.domserv.mapper.FriendMapper;
import com.chat.domserv.mapper.UserMapper;
import com.chat.domserv.usecase.FriendCommandUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class FriendCommandService implements FriendCommandUseCase {
	private final FriendMapper friendMapper;
	private final UserMapper userMapper;

	// ====== 친구 추가 요청 ===============================================================================================================================
	@Override
	public boolean addFriend(Long myUserId, String targetPublicId) {
		Long targetUserId = userMapper.findUserIdByPublicId(targetPublicId);

		if (targetUserId == null || (myUserId.equals(targetUserId))) {
			return false;
		}

		try {
			int addedFriend = friendMapper.addFriend(myUserId, targetUserId);
			return addedFriend > 0;

		} catch (DuplicateKeyException e) {
			log.info("이미 친구이거나 친구 요청 존재: myUserId={}, targetUserId={}", myUserId, targetUserId);
			throw e;
		}
	}

	// ====== 친구 수락/거절 응답 ======================================================================================================================
	@Override
	public boolean respondFriendRequest(Long myUserId, String requesterPublicId, String action) {
		Long requesterUserId = userMapper.findUserIdByPublicId(requesterPublicId);

		if (requesterUserId == null || myUserId.equals(requesterUserId)) {
			return false;
		}

		String nextStatus;

		if ("ACCEPT".equals(action)) {
			nextStatus = "ACCEPTED";
		} else if ("REJECT".equals(action)) {
			nextStatus = "REJECTED";
		} else {
			return false;
		}

		int updated = friendMapper.respondFriendRequest(myUserId, requesterUserId, nextStatus);

		return updated > 0;
	}
}
