package com.chat.domserv.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.domserv.mapper.ChatMapper;
import com.chat.domserv.usecase.ChatQueryUseCase;
import com.chat.redis.cache.RoomReadPositionCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChatQueryService implements ChatQueryUseCase {
	private final ChatMapper chatMapper;

	private final RoomReadPositionCache roomReadPositionCache;

	@Override
	@Transactional
	public List<ChatMessageViewDTO> loadMessagesInRoom(Long roomId) {

		List<ChatMessageViewDTO> chatList = chatMapper.loadMessagesInRoom(roomId);
		return chatList;
	}
}
