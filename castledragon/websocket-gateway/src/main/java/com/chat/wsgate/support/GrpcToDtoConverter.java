package com.chat.wsgate.support;

import java.time.LocalDateTime;

import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.DeleteChatMessageResponseDTO;
import com.chat.contract.domain.chatting.ReactChatMessageEventResponseDTO;
import com.chat.contract.domain.chatting.ReadPositionUpdateResponseDTO;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;
import com.chat.contract.grpc.ApplyRoomNoticeResponse;
import com.chat.contract.grpc.CreateChatMessageResponse;
import com.chat.contract.grpc.DeleteChatMessageResponse;
import com.chat.contract.grpc.ReactChatMessageResponse;
import com.chat.contract.grpc.ReadChatMessageResponse;

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
		deletedMsg.setDeleterPublicId(response.getDeleterPublicId());
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
		reactionEvent.setReactorPublicId(response.getReactorPublicId());
		reactionEvent.setReactionType(response.getReactionType());
		reactionEvent.setReactionCode(response.getReactionCode());
		reactionEvent.setAdded(response.getAdded());

		if (response.getReactedAt() != null && !response.getReactedAt().isBlank()) {
			reactionEvent.setReactedAt(LocalDateTime.parse(response.getReactedAt()));
		}

		return reactionEvent;
	}

	public static RoomNoticeViewResponseDTO convertGrpcToRoomNoticeViewResDto(ApplyRoomNoticeResponse response) {
		RoomNoticeViewResponseDTO roomNoticeView = new RoomNoticeViewResponseDTO();

		roomNoticeView.setRoomNoticeId(response.getRoomNoticeId());
		roomNoticeView.setRoomId(response.getRoomId());
		roomNoticeView.setRoomNoticeAction(response.getRoomNoticeAction());
		roomNoticeView.setRoomNoticeType(response.getRoomNoticeType());
		roomNoticeView.setRoomNoticeContents(response.getRoomNoticeContents());
		roomNoticeView.setRoomNoticeStatus(response.getRoomNoticeStatus());
		roomNoticeView.setApplierPublicId(response.getApplierPublicId());

		if (response.getLastAppliedAt() != null && !response.getLastAppliedAt().isBlank()) {
			roomNoticeView.setLastAppliedAt(LocalDateTime.parse(response.getLastAppliedAt()));
		}

		return roomNoticeView;
	}
}

