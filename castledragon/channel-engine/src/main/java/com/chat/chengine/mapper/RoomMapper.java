package com.chat.chengine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.chat.contract.room.command.ApplyRoomNoticeCommand;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomMemberResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewDTO;

@Mapper
public interface RoomMapper {
	Long lockRoomForUpdate(@Param("roomId") Long roomId);

	Long findActiveRoomNoticeId(@Param("roomId") Long roomId);

	String findRoomNoticeStatus(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	Long findRoomNoticeRequesterUserId(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	int insertRoomNotice(ApplyRoomNoticeCommand cmd);

	int updateRoomNotice(ApplyRoomNoticeCommand cmd);

	int inactivateActiveRoomNotice(@Param("roomId") Long roomId, @Param("requesterUserId") Long requesterUserId);

	int inactivateRoomNotice(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	int reactivateRoomNotice(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	int deleteRoomNotice(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	RoomNoticeViewDTO findLatestRoomNoticeView(@Param("roomId") Long roomId);

	RoomNoticeViewDTO findRoomNoticeViewById(@Param("roomId") Long roomId, @Param("roomNoticeId") Long roomNoticeId);

	EnterRoomResponseDTO findDirectRoomForEnter(@Param("requesterUserId") Long requesterUserId, @Param("friendPublicId") String friendPublicId);

	EnterRoomResponseDTO findRoomForEnter(@Param("roomId") Long roomId, @Param("requesterUserId") Long requesterUserId);

	List<RoomMemberResponseDTO> findRoomMemberProfiles(@Param("roomId") Long roomId);

	RoomNoticeViewDTO findActiveRoomNoticeView(@Param("roomId") Long roomId);

	String findNicknameByUserId(@Param("userId") Long userId);

	String findNicknameByPublicId(@Param("publicId") String publicId);

	List<String> findNicknamesByPublicIds(@Param("publicIds") List<String> publicIds);

	String findActiveRoomMemberRole(@Param("roomId") Long roomId, @Param("userId") Long userId);

	int leftRoom(@Param("roomId") Long roomId, @Param("requesterUserId") Long requesterUserId);

	int inviteMembers(@Param("roomId") Long roomId, @Param("targetPublicIds") List<String> targetPublicIds); // 좀 무거운 편이다.

	int kickMember(@Param("roomId") Long roomId, @Param("requesterUserId") Long requesterUserId, @Param("targetPublicId") String targetPublicId);

	int banMember(@Param("roomId") Long roomId, @Param("requesterUserId") Long requesterUserId, @Param("targetPublicId") String targetPublicId);

	int changeMemberRole(@Param("roomId") Long roomId, @Param("requesterUserId") Long requesterUserId, @Param("targetPublicId") String targetPublicId, @Param("targetRole") String targetRole);
}
