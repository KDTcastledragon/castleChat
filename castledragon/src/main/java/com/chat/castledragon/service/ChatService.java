package com.chat.castledragon.service;

import java.util.List;

import com.chat.castledragon.domain.ChatDTO;

public interface ChatService {

	List<ChatDTO> getListWithFri(String user_id, String fri_id);

	void sendMessage(String sender_id, String receiver_id, String msg);

}
