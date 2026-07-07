package com.chat.wsgate.support;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.chatting.domain.res.DeleteChatMessageResponseDTO;
import com.chat.contract.chatting.domain.res.ReactChatMessageEventResponseDTO;
import com.chat.contract.chatting.domain.res.ReadPositionUpdateResponseDTO;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.CreateChatMessageResponse;
import com.chat.contract.grpc.DeleteChatMessageResponse;
import com.chat.contract.grpc.EnterRoomResponse;
import com.chat.contract.grpc.FriendEventResponse;
import com.chat.contract.grpc.ReactChatMessageResponse;
import com.chat.contract.grpc.ReadChatMessageResponse;
import com.chat.contract.grpc.RoomFeedResponse;
import com.chat.contract.grpc.RoomMemberProfile;
import com.chat.contract.grpc.RoomNoticeView;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomFeedResponseDTO;
import com.chat.contract.room.domain.res.RoomMemberResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeApplyResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewDTO;

public final class GrpcToDtoConverter {
	private GrpcToDtoConverter() {
	}

	public static ChatMessageViewResponseDTO convertGrpcToChatMsgViewDto(CreateChatMessageResponse response) {
		ChatMessageViewResponseDTO chatMsgView = new ChatMessageViewResponseDTO();

		chatMsgView.setMessageId(response.getMessageId());
		chatMsgView.setRoomId(response.getRoomId());
		chatMsgView.setSenderPublicId(response.getSenderPublicId());
		chatMsgView.setMessageType(response.getMessageType());
		chatMsgView.setMessageText(response.getMessageText());
		chatMsgView.setReplyToMessageId(response.getReplyToMessageId());
		chatMsgView.setCreatedAt(LocalDateTime.parse(response.getCreatedAt()));
		chatMsgView.setUnreadCount(response.getUnreadCount());

		// attachments 변환은 ChatAttachmentDTO 필드명 맞춰서 추가 필요
		// 지금 당장 compile 우선이면 빈 리스트/후속 구현으로 둬도 됨.

		return chatMsgView;
	}

	public static ReadPositionUpdateResponseDTO convertGrpcToReadPosUpdateResDto(ReadChatMessageResponse response) {
		ReadPositionUpdateResponseDTO readPosition = new ReadPositionUpdateResponseDTO();

		readPosition.setRoomId(response.getRoomId());
		readPosition.setReaderPublicId(response.getReaderPublicId());
		readPosition.setOldLastReadMessageId(response.getOldLastReadMessageId());
		readPosition.setLastReadMessageId(response.getLastReadMessageId());
		readPosition.setUpdated(response.getUpdated());

		return readPosition;
	}

	public static DeleteChatMessageResponseDTO convertGrpcToDeleteChatMsgResDto(DeleteChatMessageResponse response) {
		DeleteChatMessageResponseDTO deletedMsg = new DeleteChatMessageResponseDTO();

		deletedMsg.setRoomId(response.getRoomId());
		deletedMsg.setMessageId(response.getMessageId());
		deletedMsg.setRequesterPublicId(response.getRequesterPublicId());
		deletedMsg.setMessageStatus(response.getMessageStatus());

		if (response.getDeletedAt() != null && !response.getDeletedAt().isBlank()) {
			deletedMsg.setDeletedAt(LocalDateTime.parse(response.getDeletedAt()));
		}

		return deletedMsg;
	}

	public static ReactChatMessageEventResponseDTO convertGrpcToReactChatMsgEventResDto(ReactChatMessageResponse response) {
		ReactChatMessageEventResponseDTO reactionEvent = new ReactChatMessageEventResponseDTO();

		reactionEvent.setRoomId(response.getRoomId());
		reactionEvent.setMessageId(response.getMessageId());
		reactionEvent.setRequesterPublicId(response.getRequesterPublicId());
		reactionEvent.setReactionType(response.getReactionType());
		reactionEvent.setReactionCode(response.getReactionCode());
		reactionEvent.setAdded(response.getAdded());

		if (response.getReactedAt() != null && !response.getReactedAt().isBlank()) {
			reactionEvent.setReactedAt(LocalDateTime.parse(response.getReactedAt()));
		}

		return reactionEvent;
	}

	public static EnterRoomResponseDTO convertGrpcToEnterRoomResDto(EnterRoomResponse response) {
		EnterRoomResponseDTO enterRoom = new EnterRoomResponseDTO();

		enterRoom.setRoomId(response.getRoomId());
		enterRoom.setRoomType(response.getRoomType());
		enterRoom.setCustomRoomName(response.getRoomName());
		enterRoom.setCustomRoomThumbnail(response.getRoomThumbnail());
		enterRoom.setCustomRoomBackground(response.getRoomBackground());
		enterRoom.setRoomMemberCount(response.getRoomMemberCount());

		List<RoomMemberResponseDTO> members = new ArrayList<>();
		for (RoomMemberProfile profile : response.getMemberListList()) {
			RoomMemberResponseDTO member = new RoomMemberResponseDTO();

			member.setPublicId(profile.getPublicId());
			member.setNickname(profile.getNickname());
			member.setFriendCode(profile.getFriendCode());
			member.setProfileImg(profile.getProfileImg());
			member.setRole(profile.getRole());

			members.add(member);
		}

		enterRoom.setMemberList(members);

		if (response.hasActiveRoomNotice()) {
			enterRoom.setRoomNotice(convertGrpcToRoomNoticeViewDto(response.getActiveRoomNotice()));
		}

		return enterRoom;
	}

	private static RoomNoticeViewDTO convertGrpcToRoomNoticeViewDto(RoomNoticeView response) {
		RoomNoticeViewDTO roomNoticeView = new RoomNoticeViewDTO();

		roomNoticeView.setRoomNoticeId(response.getRoomNoticeId());
		roomNoticeView.setRoomId(response.getRoomId());
		roomNoticeView.setRoomNoticeAction(response.getRoomNoticeAction());
		roomNoticeView.setRoomNoticeType(response.getRoomNoticeType());
		roomNoticeView.setRoomNoticeContents(response.getRoomNoticeContents());
		roomNoticeView.setRoomNoticeStatus(response.getRoomNoticeStatus());
		roomNoticeView.setRequesterPublicId(response.getRequesterPublicId());

		if (response.getLastAppliedAt() != null && !response.getLastAppliedAt().isBlank()) {
			roomNoticeView.setLastAppliedAt(LocalDateTime.parse(response.getLastAppliedAt()));
		}

		return roomNoticeView;
	}

	public static RoomFeedResponseDTO convertGrpcToRoomFeedResDto(RoomFeedResponse response) {
		RoomFeedResponseDTO roomFeed = new RoomFeedResponseDTO();

		roomFeed.setRoomId(response.getRoomId());
		roomFeed.setFeedType(response.getFeedType());
		roomFeed.setRequesterPublicId(response.getRequesterPublicId());
		roomFeed.setRequesterNickname(response.getRequesterNickname());
		roomFeed.setTargetPublicIds(response.getTargetPublicIdsList());
		roomFeed.setTargetNicknames(response.getTargetNicknamesList());
		roomFeed.setFeedText(response.getFeedText());

		if (response.getFeedAt() != null && !response.getFeedAt().isBlank()) {
			roomFeed.setFeedAt(LocalDateTime.parse(response.getFeedAt()));
		}

		return roomFeed;
	}

	public static RoomNoticeApplyResponseDTO convertGrpcToRoomNoticeApplyResDto(ApplyRoomNoticeResponse response) {
		RoomNoticeApplyResponseDTO roomNoticeApply = new RoomNoticeApplyResponseDTO();

		roomNoticeApply.setRoomNoticeView(convertGrpcToRoomNoticeViewDto(response.getRoomNoticeView()));
		roomNoticeApply.setRoomFeedResponse(convertGrpcToRoomFeedResDto(response.getRoomFeed()));

		return roomNoticeApply;
	}

	public static FriendEventResponseDTO convertGrpcToFriendEventResDto(FriendEventResponse response) {
		FriendEventResponseDTO friendEvent = new FriendEventResponseDTO();

		friendEvent.setFriendEventType(response.getFriendEventType());
		friendEvent.setRequesterUserId(response.getRequesterUserId());
		friendEvent.setRequesterPublicId(response.getRequesterPublicId());
		friendEvent.setTargetUserId(response.getTargetUserId());
		friendEvent.setTargetPublicId(response.getTargetPublicId());
		friendEvent.setFriendStatus(response.getFriendStatus());

		if (response.getEventAt() != null && !response.getEventAt().isBlank()) {
			friendEvent.setEventAt(LocalDateTime.parse(response.getEventAt()));
		}

		return friendEvent;
	}
}
