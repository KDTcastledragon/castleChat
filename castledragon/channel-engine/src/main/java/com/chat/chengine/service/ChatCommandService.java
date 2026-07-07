package com.chat.chengine.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chengine.mapper.ChatMapper;
import com.chat.chengine.usecase.ChatCommandUseCase;
import com.chat.contract.chatting.command.CreateChatMessageCommand;
import com.chat.contract.chatting.command.DeleteChatMessageCommand;
import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.command.ReadChatMessageCommand;
import com.chat.contract.chatting.command.StartDirectChatCommand;
import com.chat.contract.chatting.command.StartGroupChatCommand;
import com.chat.contract.chatting.domain.ChatMessagesDTO;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.chatting.domain.res.DeleteChatMessageResponseDTO;
import com.chat.contract.chatting.domain.res.ReactChatMessageEventResponseDTO;
import com.chat.contract.chatting.domain.res.ReadPositionUpdateResponseDTO;
import com.chat.contract.room.domain.ChatRoomsDTO;
import com.chat.contract.room.domain.ChatUserLookupDTO;
import com.chat.redis.cache.ReadPositionUpdateResult;
import com.chat.redis.cache.RoomMemberCache;
import com.chat.redis.cache.RoomReadPositionCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChatCommandService implements ChatCommandUseCase {

	//	@Autowired 빼는 이유 : 필수 의존성이 명확함. final 가능. 테스트 쉬움. Spring 권장 방식. 객체 생성 시점에 의존성 누락을 바로 알 수 있음
	private final ChatMapper chatMapper;

	private final RoomMemberCache roomMemberCache;
	private final RoomReadPositionCache roomReadPositionCache;

	private static final String DIRECT = "DIRECT";
	private static final String GROUP = "GROUP";

	private static final String ACTIVE = "ACTIVE";

	private static final String MEMBER = "MEMBER";
	private static final String HOST = "HOST";

	private ChatRoomsDTO createRoom(String roomType, String roomStatus, String roomName, Long createdBy) {
		ChatRoomsDTO room = new ChatRoomsDTO();
		room.setRoomType(roomType);
		room.setRoomStatus(roomStatus);
		room.setRoomName(roomName);
		room.setCreatedBy(createdBy);

		int created = chatMapper.createRoom(room);

		if (created != 1) {
			throw new IllegalStateException("채팅방 생성 실패");
		}

		return room;
	}

	private void validateSendBody(String messageType, String messageText, List<Long> attachmentIds) {
		if (!hasText(messageType)) {
			throw new IllegalArgumentException("messageType이 없습니다.");
		}

		boolean hasMessageText = hasText(messageText);
		boolean hasAttachment = attachmentIds != null && !attachmentIds.isEmpty();

		if (!hasMessageText && !hasAttachment) {
			throw new IllegalArgumentException("메시지 내용 또는 첨부파일이 필요합니다.");
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public ChatMessageViewResponseDTO startDirectChat(StartDirectChatCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("startDirectChat 요청이 없습니다.");
		}

		if (!hasText(cmd.getTargetPublicId())) {
			throw new IllegalArgumentException("targetPublicId가 없습니다.");
		}

		if (cmd.getSenderUserId() == null) {
			throw new IllegalArgumentException("senderUserId가 없습니다.");
		}

		if (!hasText(cmd.getSenderPublicId())) {
			throw new IllegalArgumentException("senderPublicId가 없습니다.");
		}

		validateSendBody(cmd.getMessageType(), cmd.getMessageText(), cmd.getAttachmentIds());

		ChatUserLookupDTO targetUser = chatMapper.findUserInfoByPublicId(cmd.getTargetPublicId());

		if (targetUser == null) {
			throw new IllegalArgumentException("존재하지 않는 상대입니다.");
		}

		ChatUserLookupDTO senderUser = chatMapper.findUserInfoByPublicId(cmd.getSenderPublicId());

		if (senderUser == null) {
			throw new IllegalArgumentException("보낸 사람 정보를 찾을 수 없습니다.");
		}

		ChatRoomsDTO room = chatMapper.findDirectRoom(cmd.getSenderUserId(), targetUser.getUserId());

		if (room == null) {
			room = createRoom(DIRECT, ACTIVE, "D:" + cmd.getSenderUserId() + ":" + targetUser.getUserId(), cmd.getSenderUserId());

			chatMapper.insertRoomMember(room.getRoomId(), cmd.getSenderUserId(), MEMBER, targetUser.getNickname() + "님과의 채팅방", targetUser
					.getProfileImg(), null, ACTIVE);

			chatMapper.insertRoomMember(room.getRoomId(), targetUser.getUserId(), MEMBER, senderUser.getNickname() + "님과의 채팅방", senderUser
					.getProfileImg(), null, ACTIVE);

			roomMemberCache.initOrReplaceRoomMembers(room.getRoomId(), Set.of(cmd.getSenderUserId(), targetUser.getUserId()));
		} else {
			List<Long> directMemberIds = List.of(cmd.getSenderUserId(), targetUser.getUserId());

			chatMapper.reactivateRoomMembers(room.getRoomId(), directMemberIds);

			Set<Long> cachedMemberIds = roomMemberCache.getRoomMembers(room.getRoomId());
			Set<Long> expectedMemberIds = new HashSet<>(directMemberIds);

			if (cachedMemberIds == null || cachedMemberIds.isEmpty()) {
				roomMemberCache.initOrReplaceRoomMembers(room.getRoomId(), expectedMemberIds);
			} else {
				Set<Long> missingMemberIds = new HashSet<>(expectedMemberIds);
				missingMemberIds.removeAll(cachedMemberIds);

				if (!missingMemberIds.isEmpty()) {
					roomMemberCache.addRoomMembers(room.getRoomId(), missingMemberIds);
				}
			}
		}

		CreateChatMessageCommand createCmd = new CreateChatMessageCommand(room.getRoomId(), cmd.getSenderUserId(), cmd.getSenderPublicId(), cmd
				.getMessageType(), cmd.getMessageText(), cmd.getReplyToMessageId(), cmd.getAttachmentIds());

		return createChatMessage(createCmd);
	}

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public ChatMessageViewResponseDTO startGroupChat(StartGroupChatCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("startGroupChat 요청이 없습니다.");
		}

		if (cmd.getSenderUserId() == null) {
			throw new IllegalArgumentException("senderUserId가 없습니다.");
		}

		if (!hasText(cmd.getSenderPublicId())) {
			throw new IllegalArgumentException("senderPublicId가 없습니다.");
		}

		if (cmd.getInviteMemberPublicIds() == null || cmd.getInviteMemberPublicIds().isEmpty()) {
			throw new IllegalArgumentException("초대 멤버가 없습니다.");
		}

		validateSendBody(cmd.getMessageType(), cmd.getMessageText(), cmd.getAttachmentIds());

		ChatUserLookupDTO senderUser = chatMapper.findUserInfoByPublicId(cmd.getSenderPublicId());

		if (senderUser == null) {
			throw new IllegalArgumentException("보낸 사람 정보를 찾을 수 없습니다.");
		}

		List<ChatUserLookupDTO> inviteMembers = chatMapper.findUserInfoByPublicIdList(cmd.getInviteMemberPublicIds());

		if (inviteMembers == null || inviteMembers.size() != new HashSet<>(cmd.getInviteMemberPublicIds()).size()) {
			throw new IllegalArgumentException("존재하지 않는 초대 대상이 포함되어 있습니다.");
		}

		String roomName = hasText(cmd.getRoomName()) ? cmd.getRoomName() : senderUser.getNickname() + "님의 채팅방";

		ChatRoomsDTO room = createRoom(GROUP, ACTIVE, roomName, cmd.getSenderUserId());

		chatMapper.insertRoomMember(room.getRoomId(), cmd.getSenderUserId(), HOST, roomName, cmd.getRoomThumbnail(), null, ACTIVE);

		Set<Long> activeMemberIds = new HashSet<>();
		activeMemberIds.add(cmd.getSenderUserId());

		for (ChatUserLookupDTO member : inviteMembers) {
			chatMapper.insertRoomMember(room.getRoomId(), member.getUserId(), MEMBER, roomName, cmd.getRoomThumbnail(), null, ACTIVE);

			activeMemberIds.add(member.getUserId());
		}

		roomMemberCache.initOrReplaceRoomMembers(room.getRoomId(), activeMemberIds);

		CreateChatMessageCommand createCmd = new CreateChatMessageCommand(room.getRoomId(), cmd.getSenderUserId(), cmd.getSenderPublicId(), cmd
				.getMessageType(), cmd.getMessageText(), cmd.getReplyToMessageId(), cmd.getAttachmentIds());

		return createChatMessage(createCmd);
	}

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public ChatMessageViewResponseDTO createChatMessage(CreateChatMessageCommand cmd) {
		// 필수 인자 여부 검증.
		if (cmd.getRoomId() == null) {
			log.error("id:{} 채팅의 roomId없음 : {}", cmd.getSenderUserId(), cmd.getRoomId());
			throw new IllegalArgumentException("No RoomId");
		}

		if (cmd.getMessageText() == null) {
			log.error("id:{} 채팅의 msg없음 : {}", cmd.getSenderUserId(), cmd.getMessageText());
			throw new IllegalArgumentException("no msg");
		}

		// 채팅방 내 모든 멤버 불러오기. 현재 sender가 실제 이 채팅방의 멤버인지 검증하기 위함.
		Set<Long> allActiveMemberIdsInRoom = roomMemberCache
				.getOrLoadRoomMembers(cmd.getRoomId(), () -> chatMapper.findAllActiveMemberIdsInRoom(cmd.getRoomId()));

		if (allActiveMemberIdsInRoom.isEmpty()) {
			log.error("id:{}의 채팅방 멤버 정보를 찾을 수 없음. {}", cmd.getSenderUserId(), allActiveMemberIdsInRoom);
			throw new IllegalArgumentException("채팅방 멤버 정보를 찾을 수 없음.");
		}

		if (!allActiveMemberIdsInRoom.contains(cmd.getSenderUserId())) {
			log.error("id:{}는 {}-채팅방의 멤버가 아닙니다. {}", cmd.getSenderUserId(), cmd.getRoomId(), allActiveMemberIdsInRoom);
			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
		}

		LocalDateTime now = LocalDateTime.now();

		ChatMessagesDTO msg = new ChatMessagesDTO();
		msg.setRoomId(cmd.getRoomId());
		msg.setSenderId(cmd.getSenderUserId());
		msg.setMessageType(cmd.getMessageType());
		msg.setMessageText(cmd.getMessageText());
		msg.setReplyToMessageId(cmd.getReplyToMessageId());
		msg.setCreatedAt(now);

		// DB insert
		int isCreated = chatMapper.insertChatMessage(msg); // useGK + keyP 설정 => msgId PK 사용 가능. 

		if (isCreated != 1) {
			throw new IllegalStateException("insert Msg Failed");
		}

		if (cmd.getAttachmentIds() != null && !cmd.getAttachmentIds().isEmpty()) {
			int attached = chatMapper.updateChatMessageAttachments(msg.getMessageId(), msg.getRoomId(), cmd.getAttachmentIds());

			if (attached != cmd.getAttachmentIds().size()) {
				throw new IllegalStateException("첨부파일 메시지 연결 실패");
			}
		}

		// ====== sender의 lastReadMsg In Room도 적용시켜준다. 단, readMsg 흐름과 다르게 독립적으로 조용히. ==============================================================
		ReadPositionUpdateResult rslt = roomReadPositionCache.updateIfGreater(cmd.getRoomId(), cmd.getSenderUserId(), msg
				.getMessageId(), () -> chatMapper.findLastReadMessageId(cmd.getRoomId(), cmd.getSenderUserId()));
		log.info("[sendMsg]redisGreater 결과 = room:{} sender:{} old:{} new:{}", cmd.getRoomId(), cmd.getSenderUserId(), rslt
				.oldLastReadMessageId(), rslt.newLastReadMessageId());

		Long unreadCount = Math.max(allActiveMemberIdsInRoom.size() - 1L, 0L); // 메시지 읽지 않은 멤버 수. (sender 제외)

		ChatMessageViewResponseDTO response = new ChatMessageViewResponseDTO();
		response.setMessageId(msg.getMessageId());
		response.setRoomId(msg.getRoomId());
		response.setSenderPublicId(cmd.getSenderPublicId());
		response.setMessageType(msg.getMessageType());
		response.setMessageText(msg.getMessageText());
		response.setReplyToMessageId(msg.getReplyToMessageId());
		response.setAttachments(List.of());
		response.setCreatedAt(msg.getCreatedAt());
		response.setUnreadCount(unreadCount);

		return response;
	}

	@Override
	@Transactional
	public ReadPositionUpdateResponseDTO readChatMessage(ReadChatMessageCommand cmd) {
		if (cmd.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (cmd.getReaderUserId() == null) {
			throw new IllegalArgumentException("readerUserId가 없습니다.");
		}

		if (cmd.getReaderPublicId() == null) {
			throw new IllegalArgumentException("readerPublicId가 없습니다.");
		}

		if (cmd.getLastReadMessageId() == null) {
			throw new IllegalArgumentException("lastReadMessageId가 없습니다.");
		}

		log.info("gRPC chengine readService cmd : {}", cmd);

		ReadPositionUpdateResult updateResult = roomReadPositionCache.updateIfGreater(cmd.getRoomId(), cmd.getReaderUserId(), cmd
				.getLastReadMessageId(), () -> chatMapper.findLastReadMessageId(cmd.getRoomId(), cmd.getReaderUserId()));
		log.info("[readMsg]redisGreater 결과 = room:{} reader:{} old:{} new:{}", cmd.getRoomId(), cmd.getReaderUserId(), updateResult
				.oldLastReadMessageId(), updateResult.newLastReadMessageId());

		log.info("gRPC chengine readService updateResult : {}", updateResult);

		return new ReadPositionUpdateResponseDTO(cmd.getRoomId(), cmd.getReaderPublicId(), updateResult.oldLastReadMessageId(), updateResult
				.newLastReadMessageId(), updateResult.updated());
	}

	@Override
	@Transactional
	public DeleteChatMessageResponseDTO deleteChatMessage(DeleteChatMessageCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("삭제 요청이 없습니다.");
		}

		if (cmd.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (cmd.getMessageId() == null) {
			throw new IllegalArgumentException("messageId가 없습니다.");
		}

		if (cmd.getRequesterUserId() == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		if (cmd.getRequesterPublicId() == null || cmd.getRequesterPublicId().isBlank()) {
			throw new IllegalArgumentException("requesterPublicId가 없습니다.");
		}

		// room row lock까지는 과하고, message row만 잠그면 됨.
		Long lockedMessageId = chatMapper.lockChatMessageForUpdate(cmd.getRoomId(), cmd.getMessageId());

		if (lockedMessageId == null) {
			throw new IllegalArgumentException("존재하지 않는 메시지입니다.");
		}

		String messageStatus = chatMapper.findChatMessageStatus(cmd.getRoomId(), cmd.getMessageId());

		if ("DELETED".equals(messageStatus)) {
			throw new IllegalStateException("이미 삭제된 메시지입니다.");
		}

		Long senderUserId = chatMapper.findChatMessageSenderUserId(cmd.getRoomId(), cmd.getMessageId());

		if (senderUserId == null) {
			throw new IllegalArgumentException("메시지 작성자 정보를 찾을 수 없습니다.");
		}

		if (!cmd.getRequesterUserId().equals(senderUserId)) {
			throw new IllegalArgumentException("메시지 작성자만 삭제할 수 있습니다.");
		}

		int updated = chatMapper.deleteChatMessage(cmd.getRoomId(), cmd.getMessageId(), cmd.getRequesterUserId());

		if (updated != 1) {
			throw new IllegalStateException("메시지 삭제 실패");
		}

		return new DeleteChatMessageResponseDTO(cmd.getRoomId(), cmd.getMessageId(), cmd.getRequesterPublicId(), "DELETED", LocalDateTime.now());
	}

	@Override
	@Transactional
	public ReactChatMessageEventResponseDTO reactChatMessage(ReactChatMessageCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("리액션 요청이 없습니다.");
		}

		if (cmd.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (cmd.getMessageId() == null) {
			throw new IllegalArgumentException("messageId가 없습니다.");
		}

		if (cmd.getRequesterUserId() == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		if (cmd.getRequesterPublicId() == null || cmd.getRequesterPublicId().isBlank()) {
			throw new IllegalArgumentException("requesterPublicId가 없습니다.");
		}

		if (cmd.getReactionType() == null || cmd.getReactionType().isBlank()) {
			throw new IllegalArgumentException("reactionType이 없습니다.");
		}

		if (cmd.getReactionCode() == null || cmd.getReactionCode().isBlank()) {
			throw new IllegalArgumentException("reactionCode가 없습니다.");
		}

		if (cmd.getAddRequested() == null) {
			throw new IllegalArgumentException("addRequested가 없습니다.");
		}

		Long lockedMessageId = chatMapper.lockChatMessageForUpdate(cmd.getRoomId(), cmd.getMessageId());

		if (lockedMessageId == null) {
			throw new IllegalArgumentException("존재하지 않는 메시지입니다.");
		}

		String messageStatus = chatMapper.findChatMessageStatus(cmd.getRoomId(), cmd.getMessageId());

		if ("DELETED".equals(messageStatus)) {
			throw new IllegalStateException("삭제된 메시지에는 리액션할 수 없습니다.");
		}

		Set<Long> allActiveMemberIdsInRoom = roomMemberCache
				.getOrLoadRoomMembers(cmd.getRoomId(), () -> chatMapper.findAllActiveMemberIdsInRoom(cmd.getRoomId()));

		if (allActiveMemberIdsInRoom.isEmpty()) {
			throw new IllegalArgumentException("채팅방 멤버 정보를 찾을 수 없습니다.");
		}

		if (!allActiveMemberIdsInRoom.contains(cmd.getRequesterUserId())) {
			throw new IllegalArgumentException("채팅방 멤버만 리액션할 수 있습니다.");
		}

		LocalDateTime now = LocalDateTime.now();

		if (Boolean.TRUE.equals(cmd.getAddRequested())) {
			int inserted = chatMapper.insertChatMessageReaction(cmd);

			if (inserted < 1) {
				throw new IllegalStateException("리액션 추가 실패");
			}

			return new ReactChatMessageEventResponseDTO(cmd.getRoomId(), cmd.getMessageId(), cmd.getRequesterPublicId(), cmd.getReactionType(), cmd
					.getReactionCode(), true, now);
		}

		int deleted = chatMapper.deleteChatMessageReaction(cmd.getRoomId(), cmd.getMessageId(), cmd.getRequesterUserId(), cmd.getReactionCode());

		if (deleted < 1) {
			throw new IllegalStateException("리액션 취소 실패");
		}

		return new ReactChatMessageEventResponseDTO(cmd.getRoomId(), cmd.getMessageId(), cmd.getRequesterPublicId(), cmd.getReactionType(), cmd
				.getReactionCode(), false, now);
	}

}
