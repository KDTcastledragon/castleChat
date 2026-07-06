package com.chat.contract.convert;

import java.time.LocalDateTime;

import com.chat.contract.domain.chatting.ChatMessageViewResponseDTO;
import com.chat.contract.domain.chatting.ReadPositionUpdateResponseDTO;
import com.chat.contract.grpc.CreateChatMessageResponse;
import com.chat.contract.grpc.ReadChatMessageResponse;

public final class GrpcToDtoConverter {
	// --> 이 클래스를 상속하지 못하게 막기 위해서, final 붙였다.

	private GrpcToDtoConverter() {
	}
	// --> 이 클래스는 new 해서 객체 만들지 마라.

	public static ChatMessageViewResponseDTO convertGrpcToChatMsgViewDto(CreateChatMessageResponse response) {
		ChatMessageViewResponseDTO convert = new ChatMessageViewResponseDTO(response.getMessageId(), response.getRoomId(), response.getSenderPublicId(), response
				.getMessageText(), LocalDateTime.parse(response.getCreatedAt()), response.getUnreadCount()); // int32 urc라서 안 맞았음.
		return convert;
	}

	public static ReadPositionUpdateResponseDTO convertGrpcToReadPosUpdateResDto(ReadChatMessageResponse response) {
		ReadPositionUpdateResponseDTO convert = new ReadPositionUpdateResponseDTO();

		convert.setRoomId(response.getRoomId());
		convert.setReaderPublicId(response.getReaderPublicId());
		convert.setOldLastReadMessageId(response.getOldLastReadMessageId());
		convert.setLastReadMessageId(response.getLastReadMessageId());
		convert.setUpdated(response.getUpdated());

		return convert;
	}
}

// final + static --> 이건 객체가 아니라 도구 함수 모음이다”라는 표시다. 객체 자체가 필요 없는 구조이기 때문. static utility로 쓰자는 뜻.