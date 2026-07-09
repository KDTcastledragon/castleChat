package com.chat.aiassist.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.aiassist.domain.RecentMessageDTO;

@Mapper
public interface AiAssistMapper {

	List<RecentMessageDTO> findRecentMessagesInRoom(@Param("roomId") Long roomId, @Param("limit") int limit);

}
