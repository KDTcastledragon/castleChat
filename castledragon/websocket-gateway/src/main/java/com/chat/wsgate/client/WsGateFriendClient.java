package com.chat.wsgate.client;

import com.chat.contract.friend.command.AddFriendCommand;
import com.chat.contract.friend.command.RespondFriendCommand;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;

public interface WsGateFriendClient {
	FriendEventResponseDTO addFriend(AddFriendCommand command);

	FriendEventResponseDTO respondFriend(RespondFriendCommand command);
}