package com.chat.wsgate.client;

import com.chat.contract.room.command.ApplyRoomNoticeCommand;
import com.chat.contract.room.command.BanMemberCommand;
import com.chat.contract.room.command.ChangeMemberRoleCommand;
import com.chat.contract.room.command.EnterRoomCommand;
import com.chat.contract.room.command.InviteMemberCommand;
import com.chat.contract.room.command.KickMemberCommand;
import com.chat.contract.room.command.LeftRoomCommand;
import com.chat.contract.room.command.OpenDirectChatRoomCommand;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomFeedResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeApplyResponseDTO;

public interface WsGateRoomClient {
	EnterRoomResponseDTO openDirectChatRoom(OpenDirectChatRoomCommand command);

	EnterRoomResponseDTO enterRoom(EnterRoomCommand command);

	RoomFeedResponseDTO leftRoom(LeftRoomCommand command);

	RoomFeedResponseDTO inviteMember(InviteMemberCommand command);

	RoomFeedResponseDTO kickMember(KickMemberCommand command);

	RoomFeedResponseDTO banMember(BanMemberCommand command);

	RoomFeedResponseDTO changeMemberRole(ChangeMemberRoleCommand command);

	RoomNoticeApplyResponseDTO applyRoomNotice(ApplyRoomNoticeCommand command);

}
