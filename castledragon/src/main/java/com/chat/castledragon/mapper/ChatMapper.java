package com.chat.castledragon.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.chat.castledragon.domain.ChatDTO;

@Mapper
public interface ChatMapper {

	List<ChatDTO> getListWithFri(String user_id, String fri_id);

	void sendMessage(String sender_id, String receiver_id, String msg);

}
