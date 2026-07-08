package com.chat.chengine.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chengine.mapper.ChatMapper;
import com.chat.chengine.mapper.RoomMapper;
import com.chat.chengine.usecase.RoomCommandUseCase;
import com.chat.contract.room.command.ApplyRoomNoticeCommand;
import com.chat.contract.room.command.BanMemberCommand;
import com.chat.contract.room.command.ChangeMemberRoleCommand;
import com.chat.contract.room.command.EnterRoomCommand;
import com.chat.contract.room.command.InviteMemberCommand;
import com.chat.contract.room.command.KickMemberCommand;
import com.chat.contract.room.command.LeftRoomCommand;
import com.chat.contract.room.command.OpenDirectChatRoomCommand;
import com.chat.contract.room.domain.ChatRoomsDTO;
import com.chat.contract.room.domain.ChatUserLookupDTO;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomFeedResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeApplyResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewDTO;
import com.chat.redis.cache.RoomMemberCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class RoomCommandService implements RoomCommandUseCase {

	private static final String CREATE = "CREATE";
	private static final String UPDATE = "UPDATE";
	private static final String INACTIVATE = "INACTIVATE";
	private static final String REACTIVATE = "REACTIVATE";
	private static final String DELETE = "DELETE";

	private static final String MESSAGE = "MESSAGE";
	private static final String CUSTOM = "CUSTOM";

	private static final String ACTIVE = "ACTIVE";
	private static final String INACTIVE = "INACTIVE";
	private static final String DELETED = "DELETED";

	private static final String DIRECT = "DIRECT";
	private static final String MEMBER = "MEMBER";

	private final RoomMapper roomMapper;
	private final ChatMapper chatMapper;
	private final RoomMemberCache roomMemberCache;

	@Override
	@Transactional
	public RoomNoticeApplyResponseDTO applyRoomNotice(ApplyRoomNoticeCommand cmd) {
		validateBase(cmd);

		Long lockedRoomId = roomMapper.lockRoomForUpdate(cmd.getRoomId());

		if (lockedRoomId == null) {
			throw new IllegalArgumentException("존재하지 않는 방입니다.");
		}

		RoomNoticeViewDTO roomNoticeView = switch (cmd.getRoomNoticeAction()) {
		case CREATE -> createRoomNotice(cmd);
		case UPDATE -> updateRoomNotice(cmd);
		case INACTIVATE -> inactivateRoomNotice(cmd);
		case REACTIVATE -> reactivateRoomNotice(cmd);
		case DELETE -> deleteRoomNotice(cmd);
		default -> throw new IllegalArgumentException("지원하지 않는 공지 action입니다.");
		};

		RoomFeedResponseDTO roomFeed = createNoticeRoomFeed(cmd);

		return new RoomNoticeApplyResponseDTO(roomNoticeView, roomFeed);
	}

	private RoomNoticeViewDTO createRoomNotice(ApplyRoomNoticeCommand cmd) {
		validateCreate(cmd);

		Long activeNoticeId = roomMapper.findActiveRoomNoticeId(cmd.getRoomId());

		if (activeNoticeId != null) {
			validateNoticeOwner(cmd.getRoomId(), activeNoticeId, cmd.getRequesterUserId());
			roomMapper.inactivateActiveRoomNotice(cmd.getRoomId(), cmd.getRequesterUserId());
		}

		int inserted = roomMapper.insertRoomNotice(cmd);

		if (inserted != 1) {
			throw new IllegalStateException("공지 등록 실패");
		}

		return roomMapper.findLatestRoomNoticeView(cmd.getRoomId());
	}

	private RoomNoticeViewDTO updateRoomNotice(ApplyRoomNoticeCommand cmd) {
		validateTargetNotice(cmd);
		validateContent(cmd.getRoomNoticeContents());
		validateNoticeTypeIfPresent(cmd);

		String status = findAndValidateOwner(cmd);

		if (DELETED.equals(status)) {
			throw new IllegalStateException("삭제된 공지는 수정할 수 없습니다.");
		}

		int updated = roomMapper.updateRoomNotice(cmd);

		if (updated != 1) {
			throw new IllegalStateException("공지 수정 실패");
		}

		return roomMapper.findRoomNoticeViewById(cmd.getRoomId(), cmd.getTargetRoomNoticeId());
	}

	private RoomNoticeViewDTO inactivateRoomNotice(ApplyRoomNoticeCommand cmd) {
		validateTargetNotice(cmd);

		String status = findAndValidateOwner(cmd);

		if (!ACTIVE.equals(status)) {
			throw new IllegalStateException("활성 공지만 내릴 수 있습니다.");
		}

		int updated = roomMapper.inactivateRoomNotice(cmd.getRoomId(), cmd.getTargetRoomNoticeId());

		if (updated != 1) {
			throw new IllegalStateException("공지 내림 실패");
		}

		return roomMapper.findRoomNoticeViewById(cmd.getRoomId(), cmd.getTargetRoomNoticeId());
	}

	private RoomNoticeViewDTO reactivateRoomNotice(ApplyRoomNoticeCommand cmd) {
		validateTargetNotice(cmd);

		String status = findAndValidateOwner(cmd);

		if (!INACTIVE.equals(status)) {
			throw new IllegalStateException("내려간 공지만 재등록할 수 있습니다.");
		}

		Long activeNoticeId = roomMapper.findActiveRoomNoticeId(cmd.getRoomId());

		if (activeNoticeId != null) {
			validateNoticeOwner(cmd.getRoomId(), activeNoticeId, cmd.getRequesterUserId());
			roomMapper.inactivateActiveRoomNotice(cmd.getRoomId(), cmd.getRequesterUserId());
		}

		int updated = roomMapper.reactivateRoomNotice(cmd.getRoomId(), cmd.getTargetRoomNoticeId());

		if (updated != 1) {
			throw new IllegalStateException("공지 재등록 실패");
		}

		return roomMapper.findRoomNoticeViewById(cmd.getRoomId(), cmd.getTargetRoomNoticeId());
	}

	private RoomNoticeViewDTO deleteRoomNotice(ApplyRoomNoticeCommand cmd) {
		validateTargetNotice(cmd);

		String status = findAndValidateOwner(cmd);

		if (DELETED.equals(status)) {
			throw new IllegalStateException("이미 삭제된 공지입니다.");
		}

		int updated = roomMapper.deleteRoomNotice(cmd.getRoomId(), cmd.getTargetRoomNoticeId());

		if (updated != 1) {
			throw new IllegalStateException("공지 삭제 실패");
		}

		return roomMapper.findRoomNoticeViewById(cmd.getRoomId(), cmd.getTargetRoomNoticeId());
	}

	private void validateBase(ApplyRoomNoticeCommand cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("공지 요청이 없습니다.");
		}

		if (cmd.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (!hasText(cmd.getRoomNoticeAction())) {
			throw new IllegalArgumentException("roomNoticeAction이 없습니다.");
		}

		if (cmd.getRequesterUserId() == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		if (!hasText(cmd.getRequesterPublicId())) {
			throw new IllegalArgumentException("requesterPublicId가 없습니다.");
		}
	}

	private void validateCreate(ApplyRoomNoticeCommand cmd) {
		if (!hasText(cmd.getRoomNoticeType())) {
			throw new IllegalArgumentException("roomNoticeType이 없습니다.");
		}

		validateNoticeType(cmd.getRoomNoticeType());
		validateContent(cmd.getRoomNoticeContents());

		if (MESSAGE.equals(cmd.getRoomNoticeType()) && cmd.getSourceMessageId() == null) {
			throw new IllegalArgumentException("메시지 공지는 sourceMessageId가 필요합니다.");
		}
	}

	private void validateTargetNotice(ApplyRoomNoticeCommand cmd) {
		if (cmd.getTargetRoomNoticeId() == null) {
			throw new IllegalArgumentException("targetRoomNoticeId가 없습니다.");
		}
	}

	private void validateNoticeTypeIfPresent(ApplyRoomNoticeCommand cmd) {
		if (cmd.getRoomNoticeType() == null) {
			return;
		}

		validateNoticeType(cmd.getRoomNoticeType());

		if (MESSAGE.equals(cmd.getRoomNoticeType()) && cmd.getSourceMessageId() == null) {
			throw new IllegalArgumentException("메시지 공지는 sourceMessageId가 필요합니다.");
		}
	}

	private void validateNoticeType(String roomNoticeType) {
		if (!MESSAGE.equals(roomNoticeType) && !CUSTOM.equals(roomNoticeType)) {
			throw new IllegalArgumentException("지원하지 않는 roomNoticeType입니다.");
		}
	}

	private void validateContent(String contents) {
		if (!hasText(contents)) {
			throw new IllegalArgumentException("공지 내용이 없습니다.");
		}

		if (contents.length() > 1500) {
			throw new IllegalArgumentException("공지 내용은 1500자를 초과할 수 없습니다.");
		}
	}

	private String findAndValidateOwner(ApplyRoomNoticeCommand cmd) {
		String status = roomMapper.findRoomNoticeStatus(cmd.getRoomId(), cmd.getTargetRoomNoticeId());

		if (status == null) {
			throw new IllegalArgumentException("존재하지 않는 공지입니다.");
		}

		validateNoticeOwner(cmd.getRoomId(), cmd.getTargetRoomNoticeId(), cmd.getRequesterUserId());

		return status;
	}

	private void validateNoticeOwner(Long roomId, Long roomNoticeId, Long requesterUserId) {
		Long creatorUserId = roomMapper.findRoomNoticeRequesterUserId(roomId, roomNoticeId);

		if (creatorUserId == null) {
			throw new IllegalArgumentException("공지 작성자 정보를 찾을 수 없습니다.");
		}

		if (!requesterUserId.equals(creatorUserId)) {
			throw new IllegalArgumentException("공지 작성자만 변경할 수 있습니다.");
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private ChatRoomsDTO createDirectRoom(Long requesterUserId, ChatUserLookupDTO requesterUser, ChatUserLookupDTO friendUser) {
		ChatRoomsDTO room = new ChatRoomsDTO();
		room.setRoomType(DIRECT);
		room.setRoomStatus(ACTIVE);
		room.setRoomName("D:" + requesterUserId + ":" + friendUser.getUserId());
		room.setCreatedBy(requesterUserId);

		int created = chatMapper.createRoom(room);

		if (created != 1 || room.getRoomId() == null) {
			throw new IllegalStateException("1:1 채팅방 생성 실패");
		}

		int insertedRequester = chatMapper.insertRoomMember(room.getRoomId(), requesterUserId, MEMBER, friendUser.getNickname() + "님과의 채팅방", friendUser
				.getProfileImg(), null, ACTIVE);

		int insertedFriend = chatMapper.insertRoomMember(room.getRoomId(), friendUser.getUserId(), MEMBER, requesterUser.getNickname()
				+ "님과의 채팅방", requesterUser.getProfileImg(), null, ACTIVE);

		if (insertedRequester != 1 || insertedFriend != 1) {
			throw new IllegalStateException("1:1 채팅방 멤버 생성 실패");
		}

		roomMemberCache.initOrReplaceRoomMembers(room.getRoomId(), Set.of(requesterUserId, friendUser.getUserId()));

		return room;
	}

	private void reactivateDirectMembers(Long roomId, Long requesterUserId, Long friendUserId) {
		List<Long> directMemberIds = List.of(requesterUserId, friendUserId);

		chatMapper.reactivateRoomMembers(roomId, directMemberIds);

		Set<Long> expectedMemberIds = new HashSet<>(directMemberIds);
		Set<Long> cachedMemberIds = roomMemberCache.getRoomMembers(roomId);

		if (cachedMemberIds == null || cachedMemberIds.isEmpty()) {
			roomMemberCache.initOrReplaceRoomMembers(roomId, expectedMemberIds);
			return;
		}

		Set<Long> missingMemberIds = new HashSet<>(expectedMemberIds);
		missingMemberIds.removeAll(cachedMemberIds);

		if (!missingMemberIds.isEmpty()) {
			roomMemberCache.addRoomMembers(roomId, missingMemberIds);
		}
	}

	// ========================================================================================================================================
	// ========================================================================================================================================

	@Override
	@Transactional
	public EnterRoomResponseDTO openDirectChatRoom(OpenDirectChatRoomCommand cmd) {
		if (cmd.getRequesterUserId() == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		if (!hasText(cmd.getRequesterPublicId())) {
			throw new IllegalArgumentException("requesterPublicId가 없습니다.");
		}

		if (!hasText(cmd.getFriendPublicId())) {
			throw new IllegalArgumentException("friendPublicId가 없습니다.");
		}

		ChatUserLookupDTO requesterUser = chatMapper.findUserInfoByPublicId(cmd.getRequesterPublicId());

		if (requesterUser == null || !cmd.getRequesterUserId().equals(requesterUser.getUserId())) {
			throw new IllegalArgumentException("요청자 정보를 찾을 수 없습니다.");
		}

		ChatUserLookupDTO friendUser = chatMapper.findUserInfoByPublicId(cmd.getFriendPublicId());

		if (friendUser == null) {
			throw new IllegalArgumentException("존재하지 않는 친구입니다.");
		}

		if (cmd.getRequesterUserId().equals(friendUser.getUserId())) {
			throw new IllegalArgumentException("자기 자신과 1:1 채팅방을 열 수 없습니다.");
		}

		ChatRoomsDTO room = chatMapper.findDirectRoom(cmd.getRequesterUserId(), friendUser.getUserId());

		if (room == null) {
			room = createDirectRoom(cmd.getRequesterUserId(), requesterUser, friendUser);
		} else {
			reactivateDirectMembers(room.getRoomId(), cmd.getRequesterUserId(), friendUser.getUserId());
		}

		EnterRoomResponseDTO roomInfo = roomMapper.findDirectRoomForEnter(cmd.getRequesterUserId(), cmd.getFriendPublicId());

		if (roomInfo == null) {
			throw new IllegalStateException("1:1 채팅방 입장 정보 조회 실패");
		}

		roomInfo.setMemberList(roomMapper.findRoomMemberProfiles(roomInfo.getRoomId()));
		roomInfo.setRoomNotice(roomMapper.findActiveRoomNoticeView(roomInfo.getRoomId()));

		return roomInfo;
	}

	@Override
	@Transactional(readOnly = true)
	public EnterRoomResponseDTO enterRoom(EnterRoomCommand cmd) {
		if (cmd.getRoomId() == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (cmd.getRequesterUserId() == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		EnterRoomResponseDTO roomInfo = roomMapper.findRoomForEnter(cmd.getRoomId(), cmd.getRequesterUserId());

		if (roomInfo == null) {
			throw new IllegalArgumentException("입장 가능한 방이 아닙니다.");
		}

		roomInfo.setMemberList(roomMapper.findRoomMemberProfiles(cmd.getRoomId()));
		roomInfo.setRoomNotice(roomMapper.findActiveRoomNoticeView(cmd.getRoomId()));

		return roomInfo;
	}

	@Override
	@Transactional
	public RoomFeedResponseDTO leftRoom(LeftRoomCommand cmd) {
		validateRoomRequester(cmd.getRoomId(), cmd.getRequesterUserId(), cmd.getRequesterPublicId());

		String requesterNickname = roomMapper.findNicknameByUserId(cmd.getRequesterUserId());

		int updated = roomMapper.leftRoom(cmd.getRoomId(), cmd.getRequesterUserId());

		if (updated != 1) {
			throw new IllegalStateException("방 나가기 실패");
		}

		return createRoomFeed(cmd.getRoomId(), "LEFT", cmd.getRequesterPublicId(), requesterNickname, List.of(), List.of(), requesterNickname
				+ "님이 방을 나갔습니다.");
	}

	@Override
	@Transactional
	public RoomFeedResponseDTO inviteMember(InviteMemberCommand cmd) {
		validateRoomRequester(cmd.getRoomId(), cmd.getRequesterUserId(), cmd.getRequesterPublicId());

		if (cmd.getInviteTargetMemberPublicIds() == null || cmd.getInviteTargetMemberPublicIds().isEmpty()) {
			throw new IllegalArgumentException("초대 대상이 없습니다.");
		}

		String requesterNickname = roomMapper.findNicknameByUserId(cmd.getRequesterUserId());

		int inserted = roomMapper.inviteMembers(cmd.getRoomId(), cmd.getInviteTargetMemberPublicIds());

		if (inserted < 1) {
			throw new IllegalStateException("초대 처리 실패");
		}

		List<String> targetNicknames = roomMapper.findNicknamesByPublicIds(cmd.getInviteTargetMemberPublicIds());

		return createRoomFeed(cmd.getRoomId(), "INVITE", cmd.getRequesterPublicId(), requesterNickname, cmd
				.getInviteTargetMemberPublicIds(), targetNicknames, requesterNickname + "님이 " + String.join(", ", targetNicknames) + "님을 초대했습니다.");
	}

	@Override
	@Transactional
	public RoomFeedResponseDTO kickMember(KickMemberCommand cmd) {
		validateRoomRequester(cmd.getRoomId(), cmd.getRequesterUserId(), cmd.getRequesterPublicId());

		if (!hasText(cmd.getKickTargetPublicId())) {
			throw new IllegalArgumentException("강퇴 대상이 없습니다.");
		}

		String requesterNickname = roomMapper.findNicknameByUserId(cmd.getRequesterUserId());
		String targetNickname = roomMapper.findNicknameByPublicId(cmd.getKickTargetPublicId());

		int updated = roomMapper.kickMember(cmd.getRoomId(), cmd.getRequesterUserId(), cmd.getKickTargetPublicId());

		if (updated != 1) {
			throw new IllegalStateException("강퇴 실패");
		}

		return createRoomFeed(cmd.getRoomId(), "KICK", cmd.getRequesterPublicId(), requesterNickname, List.of(cmd.getKickTargetPublicId()), List
				.of(targetNickname), requesterNickname + "님이 " + targetNickname + "님을 강퇴했습니다.");
	}

	@Override
	@Transactional
	public RoomFeedResponseDTO banMember(BanMemberCommand cmd) {
		validateRoomRequester(cmd.getRoomId(), cmd.getRequesterUserId(), cmd.getRequesterPublicId());

		if (!hasText(cmd.getBanTargetPublicId())) {
			throw new IllegalArgumentException("영구강퇴 대상이 없습니다.");
		}

		String requesterNickname = roomMapper.findNicknameByUserId(cmd.getRequesterUserId());
		String targetNickname = roomMapper.findNicknameByPublicId(cmd.getBanTargetPublicId());

		int updated = roomMapper.banMember(cmd.getRoomId(), cmd.getRequesterUserId(), cmd.getBanTargetPublicId());

		if (updated != 1) {
			throw new IllegalStateException("영구강퇴 실패");
		}

		return createRoomFeed(cmd.getRoomId(), "BAN", cmd.getRequesterPublicId(), requesterNickname, List.of(cmd.getBanTargetPublicId()), List
				.of(targetNickname), requesterNickname + "님이 " + targetNickname + "님을 영구강퇴했습니다.");
	}

	@Override
	@Transactional
	public RoomFeedResponseDTO changeMemberRole(ChangeMemberRoleCommand cmd) {
		validateRoomRequester(cmd.getRoomId(), cmd.getRequesterUserId(), cmd.getRequesterPublicId());

		if (!hasText(cmd.getTargetPublicId())) {
			throw new IllegalArgumentException("권한 변경 대상이 없습니다.");
		}

		if (!hasText(cmd.getTargetRole())) {
			throw new IllegalArgumentException("변경할 role이 없습니다.");
		}

		String requesterNickname = roomMapper.findNicknameByUserId(cmd.getRequesterUserId());
		String targetNickname = roomMapper.findNicknameByPublicId(cmd.getTargetPublicId());

		int updated = roomMapper.changeMemberRole(cmd.getRoomId(), cmd.getRequesterUserId(), cmd.getTargetPublicId(), cmd.getTargetRole());

		if (updated != 1) {
			throw new IllegalStateException("권한 변경 실패");
		}

		return createRoomFeed(cmd.getRoomId(), "ROLE_CHANGED", cmd.getRequesterPublicId(), requesterNickname, List
				.of(cmd.getTargetPublicId()), List
						.of(targetNickname), requesterNickname + "님이 " + targetNickname + "님의 권한을 " + cmd.getTargetRole() + "로 변경했습니다.");
	}

	// ========================================================================================================================================
	// ========================================================================================================================================
	private void validateRoomRequester(Long roomId, Long requesterUserId, String requesterPublicId) {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (requesterUserId == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		if (!hasText(requesterPublicId)) {
			throw new IllegalArgumentException("requesterPublicId가 없습니다.");
		}
	}

	private RoomFeedResponseDTO createNoticeRoomFeed(ApplyRoomNoticeCommand cmd) {
		String requesterNickname = roomMapper.findNicknameByUserId(cmd.getRequesterUserId());

		return createRoomFeed(cmd.getRoomId(), toNoticeFeedType(cmd.getRoomNoticeAction()), cmd.getRequesterPublicId(), requesterNickname, List
				.of(), List.of(), createNoticeFeedText(requesterNickname, cmd.getRoomNoticeAction()));
	}

	private String toNoticeFeedType(String roomNoticeAction) {
		return switch (roomNoticeAction) {
		case CREATE -> "ROOM_NOTICE_CREATED";
		case UPDATE -> "ROOM_NOTICE_UPDATED";
		case INACTIVATE -> "ROOM_NOTICE_INACTIVATED";
		case REACTIVATE -> "ROOM_NOTICE_REACTIVATED";
		case DELETE -> "ROOM_NOTICE_DELETED";
		default -> throw new IllegalArgumentException("지원하지 않는 공지 action입니다.");
		};
	}

	private String createNoticeFeedText(String requesterNickname, String roomNoticeAction) {
		return switch (roomNoticeAction) {
		case CREATE -> requesterNickname + "님이 공지를 등록했습니다.";
		case UPDATE -> requesterNickname + "님이 공지를 수정했습니다.";
		case INACTIVATE -> requesterNickname + "님이 공지를 내렸습니다.";
		case REACTIVATE -> requesterNickname + "님이 공지를 다시 등록했습니다.";
		case DELETE -> requesterNickname + "님이 공지를 삭제했습니다.";
		default -> throw new IllegalArgumentException("지원하지 않는 공지 action입니다.");
		};
	}

	private RoomFeedResponseDTO createRoomFeed(Long roomId, String feedType, String requesterPublicId, String requesterNickname, List<String> targetPublicIds, List<String> targetNicknames, String feedText) {
		return new RoomFeedResponseDTO(roomId, feedType, requesterPublicId, requesterNickname, targetPublicIds, targetNicknames, feedText, LocalDateTime
				.now());
	}
}
