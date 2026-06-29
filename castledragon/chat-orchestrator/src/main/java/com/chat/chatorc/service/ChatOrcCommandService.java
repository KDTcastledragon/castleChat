package com.chat.chatorc.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chatorc.dto.PayloadReadChatMessageResponseDTO;
import com.chat.chatorc.mapper.OrcChatMapper;
import com.chat.chatorc.usecase.ChatOrcCommandUseCase;
import com.chat.contract.command.CreateChatMessageCommand;
import com.chat.contract.domain.ChatMessageViewDTO;
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

	@Override
	@Transactional
	public ChatMessageViewDTO createChatMessage(CreateChatMessageCommand command) {
		if (command.getRoomId() == null) {
			log.error("id:{} 채팅의 roomId없음 : {}", command.getSenderUserId(), command.getRoomId());
			throw new IllegalArgumentException("No RoomId");
		}

		if (command.getMessageText() == null) {
			log.error("id:{} 채팅의 msg없음 : {}", command.getSenderUserId(), command.getMessageText());
			throw new IllegalArgumentException("no msg");
		}

		Set<Long> allActiveMemberIdsInRoom = roomMemberCache.getOrLoadRoomMembers();

		int isCreated = chatMapper.insertChatMessage(null);
		return null;
	}

	@Override
	public PayloadReadChatMessageResponseDTO readChatMessage(Long roomId, Long readerUserId, String readerPuublicId, Long newlastReadMessageId) {
		// TODO Auto-generated method stub
		return null;
	}

}
