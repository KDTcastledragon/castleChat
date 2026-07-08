package com.chat.chengine.usecase;

import com.chat.contract.room.command.ApplyRoomNoticeCommand;
import com.chat.contract.room.domain.res.RoomNoticeApplyResponseDTO;

public interface RoomNoticeCommandUseCase {

	RoomNoticeApplyResponseDTO applyRoomNotice(ApplyRoomNoticeCommand command);
}