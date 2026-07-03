package com.chat.domserv.usecase;

import java.util.List;

import com.chat.contract.domain.EnterRoomResponseDTO;
import com.chat.contract.domain.SessionUserDTO;

public interface RoomCommandUseCase { // 시스템의 상태를 바꾸는 요청. DB 상태가 바뀜. 무언가를 생성/수정/삭제”하는 use case.
	EnterRoomResponseDTO getOrCreateDirectRoom(SessionUserDTO me, String friendPublicId);

	EnterRoomResponseDTO createGroupRoom(SessionUserDTO host, String roomName, String roomThumbnail, List<String> selectedFriendPublicIdList);

	boolean leftRoom(Long roomId, SessionUserDTO me);

	int inviteGroupRoom(Long roomId, SessionUserDTO inviter, List<String> inviteTargetMemberPublicIds);

	int kickMemberInRoom(Long roomId, SessionUserDTO kicker, String kickTargetPublicId);

	int banMemberInRoom(Long roomId, SessionUserDTO kicker, String banTargetPublicId);

	int changeMemberRoleInRoom(Long roomId, SessionUserDTO me, String targetPublicId);

}
