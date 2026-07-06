package com.chat.chengine.usecase;

import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;

public interface ChEngineRoomCommandUseCase {
	RoomNoticeViewResponseDTO createRoomNotice(ApplyRoomNoticeCommand command);
}


