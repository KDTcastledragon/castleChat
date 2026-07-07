package com.chat.chengine.support;

import com.chat.contract.friend.domain.res.FriendEventResponseDTO;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.EnterRoomResponse;
import com.chat.contract.grpc.FriendEventResponse;
import com.chat.contract.grpc.RoomFeedResponse;
import com.chat.contract.grpc.RoomMemberProfile;
import com.chat.contract.grpc.RoomNoticeView;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomFeedResponseDTO;
import com.chat.contract.room.domain.res.RoomMemberResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeApplyResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewDTO;

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

	private static RoomNoticeView convertRoomNoticeViewResDtoToGrpc(RoomNoticeViewDTO dto) {
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

	public static RoomFeedResponse convertRoomFeedResDtoToGrpc(RoomFeedResponseDTO dto) {
		RoomFeedResponse.Builder builder = RoomFeedResponse.newBuilder()
				.setRoomId(dto.getRoomId())
				.setFeedType(dto.getFeedType())
				.setRequesterPublicId(dto.getRequesterPublicId())
				.setRequesterNickname(dto.getRequesterNickname())
				.setFeedText(dto.getFeedText())
				.setFeedAt(dto.getFeedAt() == null ? "" : dto.getFeedAt().toString());

		if (dto.getTargetPublicIds() != null) {
			builder.addAllTargetPublicIds(dto.getTargetPublicIds());
		}

		if (dto.getTargetNicknames() != null) {
			builder.addAllTargetNicknames(dto.getTargetNicknames());
		}

		return builder.build();
	}

	public static ApplyRoomNoticeResponse convertRoomNoticeApplyResDtoToGrpc(RoomNoticeApplyResponseDTO dto) {
		return ApplyRoomNoticeResponse.newBuilder()
				.setRoomNoticeView(convertRoomNoticeViewResDtoToGrpc(dto.getRoomNoticeView()))
				.setRoomFeed(convertRoomFeedResDtoToGrpc(dto.getRoomFeedResponse()))
				.build();
	}

	public static FriendEventResponse convertFriendEventResDtoToGrpc(FriendEventResponseDTO dto) {
		return FriendEventResponse.newBuilder()
				.setFriendEventType(dto.getFriendEventType())
				.setRequesterUserId(dto.getRequesterUserId())
				.setRequesterPublicId(dto.getRequesterPublicId())
				.setTargetUserId(dto.getTargetUserId())
				.setTargetPublicId(dto.getTargetPublicId())
				.setFriendStatus(dto.getFriendStatus())
				.setEventAt(dto.getEventAt() == null ? "" : dto.getEventAt().toString())
				.build();
	}
}
