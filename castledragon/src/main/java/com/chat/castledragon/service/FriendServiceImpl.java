package com.chat.castledragon.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.chat.castledragon.domain.UserProfileResponseDTO;
import com.chat.castledragon.mapper.FriendMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class FriendServiceImpl implements FriendService {
	@Autowired
	FriendMapper friendMapper;

	@Override
	public boolean addFriend(Long myUserId, String targetPublicId) {
		Long targetUserId = friendMapper.findUserIdByPublicId(targetPublicId);

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

	@Override
	public List<UserProfileResponseDTO> getFriendList(Long userId) {
		List<UserProfileResponseDTO> list = friendMapper.getFriendList(userId);
		log.info("{}의 현재 친구 목록 : {}", userId, list);
		return (list);
	}

	@Override
	public List<UserProfileResponseDTO> getReceivedFriendRequests(Long userId) {
		List<UserProfileResponseDTO> list = friendMapper.getReceivedFriendRequests(userId);
		log.info("{}의 현재 친구 목록 : {}", userId, list);

		return (list);
	}

	@Override
	public boolean respondFriendRequest(Long myUserId, String requesterPublicId, String action) {
		Long requesterUserId = friendMapper.findUserIdByPublicId(requesterPublicId);

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

}//Impl끝.
