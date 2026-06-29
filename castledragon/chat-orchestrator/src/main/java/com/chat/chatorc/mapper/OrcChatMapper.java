package com.chat.chatorc.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.chat.contract.domain.ChatMessagesDTO;

@Mapper
public interface OrcChatMapper {
	int insertChatMessage(ChatMessagesDTO dto);
}
