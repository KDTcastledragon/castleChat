package com.chat.domserv.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chat.contract.user.domain.UserProfileResponseDTO;
import com.chat.domserv.mapper.FriendMapper;
import com.chat.domserv.usecase.FriendQueryUseCase;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class FriendQueryService implements FriendQueryUseCase {
	@Autowired
	FriendMapper friendMapper;

	@Override
	public List<UserProfileResponseDTO> getFriendList(Long userId) {
		List<UserProfileResponseDTO> list = friendMapper.getFriendList(userId);
		log.info("{}의 현재 친구 목록 : {}", userId, list);
		return (list);
	}

	@Override
	public List<UserProfileResponseDTO> getReceivedFriendRequests(Long userId) {
		List<UserProfileResponseDTO> list = friendMapper.getReceivedFriendRequests(userId);
		log.info("{}의 현재 친구추가 요청 목록 : {}", userId, list);

		return (list);
	}

}
