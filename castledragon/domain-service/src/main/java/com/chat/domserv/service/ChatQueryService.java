package com.chat.domserv.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.domserv.mapper.ChatMapper;
import com.chat.domserv.usecase.ChatQueryUseCase;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ChatQueryService implements ChatQueryUseCase {

	@Autowired
	ChatMapper chatMapper;

	@Override
	@Transactional
	public List<ChatMessageViewDTO> loadMessagesInRoom(Long roomId) {
		List<ChatMessageViewDTO> chatList = chatMapper.loadMessagesInRoom(roomId);
		return chatList;
	}
}
