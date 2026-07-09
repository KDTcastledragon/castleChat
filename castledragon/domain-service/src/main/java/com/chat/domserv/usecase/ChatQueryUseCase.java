package com.chat.domserv.usecase;

import java.util.List;
import java.util.Map;

import com.chat.contract.chatting.domain.res.ChatMessageReactionMemberResponseDTO;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.room.domain.res.RoomMemberResponseDTO;

public interface ChatQueryUseCase {
	List<ChatMessageViewResponseDTO> loadMessagesInRoom(Long roomId, Long beforeMessageId, int limit);

	List<ChatMessageReactionMemberResponseDTO> findMessageReactionMembers(Long roomId, Long messageId, Long requesterUserId);

	List<RoomMemberResponseDTO> findMessageReaders(Long roomId, Long messageId, Long requesterUserId);

	Map<Long, Long> findMessageUnreadCounts(Long roomId, List<Long> messageIds, Long requesterUserId);
}
