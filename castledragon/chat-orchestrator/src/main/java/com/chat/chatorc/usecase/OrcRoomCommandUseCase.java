package com.chat.chatorc.usecase;

import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;

public interface OrcRoomCommandUseCase {
	RoomNoticeViewResponseDTO createRoomNotice(ApplyRoomNoticeCommand command);
}
