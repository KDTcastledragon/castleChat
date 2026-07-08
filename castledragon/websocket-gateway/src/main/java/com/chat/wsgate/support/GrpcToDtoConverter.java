package com.chat.wsgate.support;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.chat.contract.chatting.domain.ChatAttachmentDTO;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.chatting.domain.res.DeleteChatMessageResponseDTO;
import com.chat.contract.chatting.domain.res.ReactChatMessageEventResponseDTO;
import com.chat.contract.chatting.domain.res.ReadPositionUpdateResponseDTO;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;
import com.chat.contract.friend.domain.res.OnlineFriendTargetsResponseDTO;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.ChatAttachment;
import com.chat.contract.grpc.CreateChatMessageResponse;
import com.chat.contract.grpc.DeleteChatMessageResponse;
import com.chat.contract.grpc.DirectChatDraft;
import com.chat.contract.grpc.EnterRoomResponse;
import com.chat.contract.grpc.FriendEventResponse;
import com.chat.contract.grpc.OnlineFriendTargetsResponse;
import com.chat.contract.grpc.OpenDirectChatRoomResponse;
import com.chat.contract.grpc.ReactChatMessageResponse;
import com.chat.contract.grpc.ReadChatMessageResponse;
import com.chat.contract.grpc.RoomFeedResponse;
import com.chat.contract.grpc.RoomMemberProfile;
import com.chat.contract.grpc.RoomNoticeView;
import com.chat.contract.grpc.StartChatResponse;
import com.chat.contract.chatting.domain.res.StartChatResponseDTO;
import com.chat.contract.room.domain.res.DirectChatDraftDTO;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.OpenDirectChatRoomResponseDTO;
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
		if (response.hasReplyToMessageId()) {
			chatMsgView.setReplyToMessageId(response.getReplyToMessageId());
		}
		chatMsgView.setCreatedAt(LocalDateTime.parse(response.getCreatedAt()));
		chatMsgView.setUnreadCount(response.getUnreadCount());
		chatMsgView.setNotificationTargetUserIds(response.getNotificationTargetUserIdsList());
		chatMsgView.setAttachments(convertGrpcToChatAttachmentDtoList(response.getAttachmentsList()));

		return chatMsgView;
	}

	private static List<ChatAttachmentDTO> convertGrpcToChatAttachmentDtoList(List<ChatAttachment> responseAttachments) {
		List<ChatAttachmentDTO> attachments = new ArrayList<>();

		for (ChatAttachment response : responseAttachments) {
			ChatAttachmentDTO attachment = new ChatAttachmentDTO();

			attachment.setAttachmentId(response.getAttachmentId());
			attachment.setMessageId(response.getMessageId() == 0L ? null : response.getMessageId());
			attachment.setRoomId(response.getRoomId());
			attachment.setUploaderUserId(response.getUploaderUserId());
			attachment.setFileUrl(response.getFileUrl());
			attachment.setOriginalFileName(response.getOriginalFileName());
			attachment.setContentType(response.getContentType());
			attachment.setFileSize(response.getFileSize());
			attachment.setAttachmentKind(response.getAttachmentKind());
			attachment.setAttachmentStatus(response.getAttachmentStatus());
			attachment.setWidth(response.getWidth() == 0 ? null : response.getWidth());
			attachment.setHeight(response.getHeight() == 0 ? null : response.getHeight());
			attachment.setDurationMs(response.getDurationMs() == 0L ? null : response.getDurationMs());
			attachment.setSortOrder(response.getSortOrder());

			attachments.add(attachment);
		}

		return attachments;
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
		enterRoom.setMessageNotificationEnabled(response.getMessageNotificationEnabled());
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

	private static DirectChatDraftDTO convertGrpcToDirectChatDraftDto(DirectChatDraft response) {
		DirectChatDraftDTO draft = new DirectChatDraftDTO();

		draft.setFriendPublicId(response.getFriendPublicId());
		draft.setFriendNickname(response.getFriendNickname());
		draft.setFriendProfileImg(response.getFriendProfileImg());

		return draft;
	}

	public static OpenDirectChatRoomResponseDTO convertGrpcToOpenDirectChatRoomResDto(OpenDirectChatRoomResponse response) {
		OpenDirectChatRoomResponseDTO openDirectChat = new OpenDirectChatRoomResponseDTO();

		openDirectChat.setRoomExists(response.getRoomExists());

		if (response.hasEnterRoomInfo()) {
			openDirectChat.setEnterRoomInfo(convertGrpcToEnterRoomResDto(response.getEnterRoomInfo()));
		}

		if (response.hasDraft()) {
			openDirectChat.setDraft(convertGrpcToDirectChatDraftDto(response.getDraft()));
		}

		return openDirectChat;
	}

	public static StartChatResponseDTO convertGrpcToStartChatResDto(StartChatResponse response) {
		StartChatResponseDTO startChat = new StartChatResponseDTO();

		startChat.setEnterRoomInfo(convertGrpcToEnterRoomResDto(response.getEnterRoomInfo()));
		startChat.setFirstChatMessage(convertGrpcToChatMsgViewDto(response.getFirstChatMessage()));

		return startChat;
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
		roomFeed.setTargetRole(response.getTargetRole());
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
		friendEvent.setRequesterNickname(response.getRequesterNickname());
		friendEvent.setTargetUserId(response.getTargetUserId());
		friendEvent.setTargetPublicId(response.getTargetPublicId());
		friendEvent.setTargetNickname(response.getTargetNickname());
		friendEvent.setFriendStatus(response.getFriendStatus());

		if (response.getEventAt() != null && !response.getEventAt().isBlank()) {
			friendEvent.setEventAt(LocalDateTime.parse(response.getEventAt()));
		}

		return friendEvent;
	}

	public static OnlineFriendTargetsResponseDTO convertGrpcToOnlineFriendTargetsResDto(OnlineFriendTargetsResponse response) {
		OnlineFriendTargetsResponseDTO onlineTargets = new OnlineFriendTargetsResponseDTO();

		onlineTargets.setUserId(response.getUserId());
		onlineTargets.setTargetUserIds(response.getTargetUserIdsList());

		return onlineTargets;
	}
}
