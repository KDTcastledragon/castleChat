package com.chat.chengine.support;

import com.chat.contract.friend.domain.res.FriendEventResponseDTO;
import com.chat.contract.friend.domain.res.OnlineFriendTargetsResponseDTO;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.EnterRoomResponse;
import com.chat.contract.grpc.FriendEventResponse;
import com.chat.contract.grpc.OnlineFriendTargetsResponse;
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

	private static String nvl(String value) {
		return value == null ? "" : value;
	}

	public static EnterRoomResponse convertEnterRoomResDtoToGrpc(EnterRoomResponseDTO dto) {
		EnterRoomResponse.Builder responseBuilder = EnterRoomResponse.newBuilder()
				.setRoomId(dto.getRoomId())
				.setRoomType(nvl(dto.getRoomType()))
				.setRoomName(nvl(dto.getCustomRoomName()))
				.setRoomThumbnail(nvl(dto.getCustomRoomThumbnail()))
				.setRoomBackground(nvl(dto.getCustomRoomBackground()))
				.setMessageNotificationEnabled(Boolean.TRUE.equals(dto.getMessageNotificationEnabled()))
				.setRoomMemberCount(dto.getRoomMemberCount() == null ? 0L : dto.getRoomMemberCount());

		if (dto.getMemberList() != null) {
			for (RoomMemberResponseDTO member : dto.getMemberList()) {
				RoomMemberProfile memberProfile = RoomMemberProfile.newBuilder()
						.setPublicId(nvl(member.getPublicId()))
						.setNickname(nvl(member.getNickname()))
						.setFriendCode(nvl(member.getFriendCode()))
						.setProfileImg(nvl(member.getProfileImg()))
						.setRole(nvl(member.getRole()))
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
				.setRoomNoticeAction(nvl(dto.getRoomNoticeAction()))
				.setRoomNoticeType(nvl(dto.getRoomNoticeType()))
				.setRoomNoticeContents(nvl(dto.getRoomNoticeContents()))
				.setRoomNoticeStatus(nvl(dto.getRoomNoticeStatus()))
				.setRequesterPublicId(nvl(dto.getRequesterPublicId()))
				.setLastAppliedAt(dto.getLastAppliedAt() == null ? "" : dto.getLastAppliedAt().toString())
				.build();
	}

	public static RoomFeedResponse convertRoomFeedResDtoToGrpc(RoomFeedResponseDTO dto) {
		RoomFeedResponse.Builder builder = RoomFeedResponse.newBuilder()
				.setRoomId(dto.getRoomId())
				.setFeedType(nvl(dto.getFeedType()))
				.setRequesterPublicId(nvl(dto.getRequesterPublicId()))
				.setRequesterNickname(nvl(dto.getRequesterNickname()))
				.setFeedText(nvl(dto.getFeedText()))
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
				.setFriendEventType(nvl(dto.getFriendEventType()))
				.setRequesterUserId(dto.getRequesterUserId())
				.setRequesterPublicId(nvl(dto.getRequesterPublicId()))
				.setRequesterNickname(nvl(dto.getRequesterNickname()))
				.setTargetUserId(dto.getTargetUserId())
				.setTargetPublicId(nvl(dto.getTargetPublicId()))
				.setTargetNickname(nvl(dto.getTargetNickname()))
				.setFriendStatus(nvl(dto.getFriendStatus()))
				.setEventAt(dto.getEventAt() == null ? "" : dto.getEventAt().toString())
				.build();
	}

	public static OnlineFriendTargetsResponse convertOnlineFriendTargetsResDtoToGrpc(OnlineFriendTargetsResponseDTO dto) {
		OnlineFriendTargetsResponse.Builder builder = OnlineFriendTargetsResponse.newBuilder()
				.setUserId(dto.getUserId());

		if (dto.getTargetUserIds() != null && !dto.getTargetUserIds().isEmpty()) {
			builder.addAllTargetUserIds(dto.getTargetUserIds());
		}

		return builder.build();
	}
}
