package com.chat.chengine.usecase;

import com.chat.contract.room.command.ApplyRoomNoticeCommand;
import com.chat.contract.room.command.OpenDirectChatRoomCommand;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeViewResponseDTO;

public interface RoomCommandUseCase {
	RoomNoticeViewResponseDTO applyRoomNotice(ApplyRoomNoticeCommand command);

	EnterRoomResponseDTO openDirectChatRoom(OpenDirectChatRoomCommand command);
}
