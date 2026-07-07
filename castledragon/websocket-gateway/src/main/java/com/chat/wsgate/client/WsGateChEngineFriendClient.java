package com.chat.wsgate.client;

import com.chat.contract.friend.command.FindOnlineFriendTargetsCommand;
import com.chat.contract.friend.domain.res.OnlineFriendTargetsResponseDTO;

public interface WsGateChEngineFriendClient {
	OnlineFriendTargetsResponseDTO findOnlineFriendTargets(FindOnlineFriendTargetsCommand command);
}
