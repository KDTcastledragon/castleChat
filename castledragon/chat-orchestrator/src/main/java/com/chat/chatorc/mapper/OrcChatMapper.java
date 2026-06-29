package com.chat.chatorc.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.chat.contract.domain.ChatMessagesDTO;

@Mapper
public interface OrcChatMapper {
	int insertChatMessage(ChatMessagesDTO dto);

	List<Long> findAllActiveMemberIdsInRoom(Long roomId);
}
