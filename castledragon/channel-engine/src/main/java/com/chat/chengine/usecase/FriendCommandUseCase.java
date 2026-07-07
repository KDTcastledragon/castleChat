package com.chat.chengine.usecase;

import com.chat.contract.friend.command.AddFriendCommand;
import com.chat.contract.friend.command.RespondFriendCommand;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;

public interface FriendCommandUseCase {
	FriendEventResponseDTO addFriend(AddFriendCommand command);

	FriendEventResponseDTO respondFriend(RespondFriendCommand command);
}