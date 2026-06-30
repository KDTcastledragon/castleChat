package com.chat.chatorc.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chatorc.dto.PayloadReadChatMessageResponseDTO;
import com.chat.chatorc.mapper.OrcChatMapper;
import com.chat.chatorc.usecase.ChatOrcCommandUseCase;
import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
import com.chat.contract.domain.ChatMessagesDTO;
import com.chat.redis.cache.RoomMemberCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChatOrcCommandService implements ChatOrcCommandUseCase {

	//	@Autowired 빼는 이유 : 필수 의존성이 명확함. final 가능. 테스트 쉬움. Spring 권장 방식. 객체 생성 시점에 의존성 누락을 바로 알 수 있음
	private final OrcChatMapper chatMapper;

	private final RoomMemberCache roomMemberCache;

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public ChatMessageViewDTO createChatMessage(CreateChatMessageCommand command) {
		// 필수 인자 여부 검증.
		if (command.getRoomId() == null) {
			log.error("id:{} 채팅의 roomId없음 : {}", command.getSenderUserId(), command.getRoomId());
			throw new IllegalArgumentException("No RoomId");
		}

		if (command.getMessageText() == null) {
			log.error("id:{} 채팅의 msg없음 : {}", command.getSenderUserId(), command.getMessageText());
			throw new IllegalArgumentException("no msg");
		}

		// 채팅방 내 모든 멤버 불러오기. 현재 sender가 실제 이 채팅방의 멤버인지 검증하기 위함.
		Set<Long> allActiveMemberIdsInRoom = roomMemberCache
				.getOrLoadRoomMembers(command.getRoomId(), () -> chatMapper.findAllActiveMemberIdsInRoom(command.getRoomId()));

		if (allActiveMemberIdsInRoom.isEmpty()) {
			log.error("id:{}의 채팅방 멤버 정보를 찾을 수 없음. {}", command.getSenderUserId(), allActiveMemberIdsInRoom);
			throw new IllegalArgumentException("채팅방 멤버 정보를 찾을 수 없음.");
		}

		if (!allActiveMemberIdsInRoom.contains(command.getSenderUserId())) {
			log.error("id:{}는 {}-채팅방의 멤버가 아닙니다. {}", command.getSenderUserId(), command.getRoomId(), allActiveMemberIdsInRoom);
			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
		}

		LocalDateTime now = LocalDateTime.now();

		ChatMessagesDTO msg = new ChatMessagesDTO();
		msg.setRoomId(command.getRoomId());
		msg.setSenderId(command.getSenderUserId());
		msg.setMessageText(command.getMessageText());
		msg.setCreatedAt(now);

		// DB insert
		int isCreated = chatMapper.insertChatMessage(msg); // useGK + keyP 설정 => msgId PK 사용 가능. 

		if (isCreated != 1) {
			throw new IllegalStateException("insert Msg Failed");
		}

		Long unreadCount = Math.max(allActiveMemberIdsInRoom.size() - 1L, 0L); // 메시지 읽지 않은 멤버 수. (sender 제외)

		ChatMessageViewDTO response = new ChatMessageViewDTO();
		response.setMessageId(msg.getMessageId());
		response.setRoomId(msg.getRoomId());
		response.setSenderPublicId(command.getSenderPublicId());
		response.setMessageText(msg.getMessageText());
		response.setCreatedAt(msg.getCreatedAt());
		response.setUnreadCount(unreadCount);

		return response;
	}

	@Override
	public PayloadReadChatMessageResponseDTO readChatMessage(Long roomId, Long readerUserId, String readerPuublicId, Long newlastReadMessageId) {
		// TODO Auto-generated method stub
		return null;
	}

}
