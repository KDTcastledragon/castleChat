package com.chat.chengine.grpc;

import com.chat.contract.grpc.EnterRoomResponse;
import com.chat.contract.grpc.RoomMemberProfile;
import com.chat.contract.grpc.RoomNoticeView;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomMemberResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewResponseDTO;

public final class DtoToGrpcConverter {
	private DtoToGrpcConverter() {
	}

	public static EnterRoomResponse convertEnterRoomResDtoToGrpc(EnterRoomResponseDTO dto) {
		EnterRoomResponse.Builder responseBuilder = EnterRoomResponse.newBuilder()
				.setRoomId(dto.getRoomId())
				.setRoomType(dto.getRoomType())
				.setRoomName(dto.getCustomRoomName())
				.setRoomThumbnail(dto.getCustomRoomThumbnail() == null ? "" : dto.getCustomRoomThumbnail())
				.setRoomBackground(dto.getCustomRoomBackground() == null ? "" : dto.getCustomRoomBackground())
				.setRoomMemberCount(dto.getRoomMemberCount());

		if (dto.getMemberList() != null) {
			for (RoomMemberResponseDTO member : dto.getMemberList()) {
				RoomMemberProfile memberProfile = RoomMemberProfile.newBuilder()
						.setPublicId(member.getPublicId())
						.setNickname(member.getNickname())
						.setFriendCode(member.getFriendCode() == null ? "" : member.getFriendCode())
						.setProfileImg(member.getProfileImg() == null ? "" : member.getProfileImg())
						.setRole(member.getRole())
						.build();

				responseBuilder.addMemberList(memberProfile);
			}
		}

		if (dto.getRoomNotice() != null) {
			responseBuilder.setActiveRoomNotice(convertRoomNoticeViewResDtoToGrpc(dto.getRoomNotice()));
		}

		return responseBuilder.build();
	}

	private static RoomNoticeView convertRoomNoticeViewResDtoToGrpc(RoomNoticeViewResponseDTO dto) {
		return RoomNoticeView.newBuilder()
				.setRoomNoticeId(dto.getRoomNoticeId())
				.setRoomId(dto.getRoomId())
				.setRoomNoticeAction(dto.getRoomNoticeAction())
				.setRoomNoticeType(dto.getRoomNoticeType())
				.setRoomNoticeContents(dto.getRoomNoticeContents())
				.setRoomNoticeStatus(dto.getRoomNoticeStatus())
				.setRequesterPublicId(dto.getRequesterPublicId())
				.setLastAppliedAt(dto.getLastAppliedAt() == null ? "" : dto.getLastAppliedAt().toString())
				.build();
	}
}
