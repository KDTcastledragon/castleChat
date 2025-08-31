package com.chat.castledragon.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chat.castledragon.domain.ChatDTO;
import com.chat.castledragon.mapper.ChatMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ChatServiceImpl implements ChatService {
	@Autowired
	ChatMapper chatmapper;

	@Override
	public List<ChatDTO> getListWithFri(String user_id, String fri_id) {
		List<ChatDTO> list = chatmapper.getListWithFri(user_id, fri_id);
		return null;
	}

	@Override
	public void sendMessage(String sender_id, String receiver_id, String msg) {
		chatmapper.sendMessage(sender_id, receiver_id, msg);
	}

}
