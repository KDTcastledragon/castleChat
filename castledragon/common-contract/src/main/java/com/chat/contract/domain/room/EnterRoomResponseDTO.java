package com.chat.contract.domain.room;

import java.util.List;

import com.chat.contract.domain.member.RoomMemberResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EnterRoomResponseDTO {
	private Long roomId;
	private String roomType;

	private String customRoomName;
	private String customRoomThumbnail;
	private String customRoomBackground;

	private Long roomMemberCount;

	private List<RoomMemberResponseDTO> memberList;

	private RoomNoticeViewResponseDTO roomNotice;
}