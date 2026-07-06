package com.chat.chengine.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;

@Mapper
public interface ChEngineRoomMapper {
	Long lockRoomForUpdate(@Param("roomId") Long roomId);

	Long findActiveRoomNoticeId(@Param("roomId") Long roomId);

	String findRoomNoticeStatus(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	Long findRoomNoticeCreatorUserId(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	int insertRoomNotice(ApplyRoomNoticeCommand cmd);

	int updateRoomNotice(ApplyRoomNoticeCommand cmd);

	int inactivateActiveRoomNotice(@Param("roomId") Long roomId, @Param("requesterUserId") Long requesterUserId);

	int inactivateRoomNotice(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	int reactivateRoomNotice(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	int deleteRoomNotice(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	RoomNoticeViewResponseDTO findLatestRoomNoticeView(@Param("roomId") Long roomId);

	RoomNoticeViewResponseDTO findRoomNoticeViewById(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);
}