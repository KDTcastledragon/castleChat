package com.chat.wsgate.client;

import com.chat.contract.command.room.ApplyRoomNoticeCommand;
import com.chat.contract.domain.room.RoomNoticeViewResponseDTO;

public interface WsGateChEngineRoomClient {

	RoomNoticeViewResponseDTO applyRoomNotice(ApplyRoomNoticeCommand command);
}
