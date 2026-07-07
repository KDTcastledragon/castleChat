package com.chat.chengine.usecase;

import com.chat.contract.friend.command.AddFriendCommand;
import com.chat.contract.friend.command.FindOnlineFriendTargetsCommand;
import com.chat.contract.friend.command.RespondFriendCommand;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;
import com.chat.contract.friend.domain.res.OnlineFriendTargetsResponseDTO;

public interface FriendCommandUseCase {
	FriendEventResponseDTO addFriend(AddFriendCommand command);

	FriendEventResponseDTO respondFriend(RespondFriendCommand command);

	OnlineFriendTargetsResponseDTO findOnlineFriendTargets(FindOnlineFriendTargetsCommand command);
}
