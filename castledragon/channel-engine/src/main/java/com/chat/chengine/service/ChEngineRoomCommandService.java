package com.chat.chengine.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chengine.mapper.ChEngineRoomMapper;
import com.chat.chengine.usecase.ChEngineRoomCommandUseCase;
import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ChEngineRoomCommandService implements ChEngineRoomCommandUseCase {

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

	private final ChEngineRoomMapper roomMapper;

	@Override
	@Transactional
	public RoomNoticeViewResponseDTO applyRoomNotice(ApplyRoomNoticeCommand cmd) {
		validateBase(cmd);

		Long lockedRoomId = roomMapper.lockRoomForUpdate(cmd.getRoomId());

		if (lockedRoomId == null) {
			throw new IllegalArgumentException("존재하지 않는 방입니다.");
		}

		return switch (cmd.getRoomNoticeAction()) {
		case CREATE -> createRoomNotice(cmd);
		case UPDATE -> updateRoomNotice(cmd);
		case INACTIVATE -> inactivateRoomNotice(cmd);
		case REACTIVATE -> reactivateRoomNotice(cmd);
		case DELETE -> deleteRoomNotice(cmd);
		default -> throw new IllegalArgumentException("지원하지 않는 공지 action입니다.");
		};
	}

	private RoomNoticeViewResponseDTO createRoomNotice(ApplyRoomNoticeCommand cmd) {
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

	private RoomNoticeViewResponseDTO updateRoomNotice(ApplyRoomNoticeCommand cmd) {
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

	private RoomNoticeViewResponseDTO inactivateRoomNotice(ApplyRoomNoticeCommand cmd) {
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

	private RoomNoticeViewResponseDTO reactivateRoomNotice(ApplyRoomNoticeCommand cmd) {
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

	private RoomNoticeViewResponseDTO deleteRoomNotice(ApplyRoomNoticeCommand cmd) {
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
		Long creatorUserId = roomMapper.findRoomNoticeCreatorUserId(roomId, roomNoticeId);

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
}