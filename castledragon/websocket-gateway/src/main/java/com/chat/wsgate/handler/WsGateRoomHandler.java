package com.chat.wsgate.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.room.command.ApplyRoomNoticeCommand;
import com.chat.contract.room.command.BanMemberCommand;
import com.chat.contract.room.command.ChangeMemberRoleCommand;
import com.chat.contract.room.command.EnterRoomCommand;
import com.chat.contract.room.command.InviteMemberCommand;
import com.chat.contract.room.command.KickMemberCommand;
import com.chat.contract.room.command.LeftRoomCommand;
import com.chat.contract.room.command.OpenDirectChatRoomCommand;
import com.chat.contract.room.domain.RoomIdDTO;
import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
import com.chat.contract.room.domain.res.OpenDirectChatRoomResponseDTO;
import com.chat.contract.room.domain.res.RoomFeedResponseDTO;
import com.chat.contract.room.domain.res.RoomNoticeApplyResponseDTO;
import com.chat.contract.user.domain.SessionUserDTO;
import com.chat.contract.websocket.domain.WebSocketDTO;
import com.chat.wsgate.auth.WsGateAuth;
import com.chat.wsgate.client.WsGateRoomClient;
import com.chat.wsgate.domain.room.PayloadApplyRoomNoticeRequestDTO;
import com.chat.wsgate.domain.room.PayloadBanMemberRequestDTO;
import com.chat.wsgate.domain.room.PayloadChangeMemberRoleRequestDTO;
import com.chat.wsgate.domain.room.PayloadExitRoomRequestDTO;
import com.chat.wsgate.domain.room.PayloadInviteMemberRequestDTO;
import com.chat.wsgate.domain.room.PayloadKickMemberRequestDTO;
import com.chat.wsgate.domain.room.PayloadOpenDirectChatRoomRequestDTO;
import com.chat.wsgate.outbound.WsGateOutboundWriter;
import com.chat.wsgate.session.WsGateSessionRegistry;
import com.chat.wsgate.support.WsGatePayloadConverter;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class WsGateRoomHandler {
	private final WsGateSessionRegistry wsGateSessionRegistry;
	private final WsGateOutboundWriter wsGateOutboundWriter;
	private final WsGateAuth wsGateAuth;

	private final WsGatePayloadConverter wsGatePayloadConverter;
	private final WsGateRoomClient wsGateRoomClient;

	// ====== 1:1 채팅방 열기 ===========================================================================================================
	public void handleOpenDirectChat(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadOpenDirectChatRoomRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadOpenDirectChatRoomRequestDTO.class);

		if (payload.getFriendPublicId() == null || payload.getFriendPublicId().isBlank()) {
			wsGateOutboundWriter.responseFail(session, dto, "OPEN_DIRECT_CHAT_FAIL", "friendPublicId 없음");
			return;
		}

		try {
			OpenDirectChatRoomCommand openDirChtCmd = new OpenDirectChatRoomCommand(me.getUserId(), me.getPublicId(), payload.getFriendPublicId());

			OpenDirectChatRoomResponseDTO openDirChtResponse = wsGateRoomClient.openDirectChatRoom(openDirChtCmd);

			if (Boolean.TRUE.equals(openDirChtResponse.getRoomExists()) && openDirChtResponse.getEnterRoomInfo() != null) {
				wsGateSessionRegistry.enterRoomSession(openDirChtResponse.getEnterRoomInfo().getRoomId(), me.getUserId(), session);
			}

			log.info("{}번 유저 direct room open. exists:{} roomId:{}", me.getUserId(), openDirChtResponse.getRoomExists(), openDirChtResponse
					.getEnterRoomInfo() == null ? null : openDirChtResponse.getEnterRoomInfo().getRoomId());

			wsGateOutboundWriter.responseOk(session, dto, "OPEN_DIRECT_CHAT_OK", openDirChtResponse);
		} catch (Exception e) {
			log.error("OPEN_DIRECT_CHAT 예외", e);
			wsGateOutboundWriter.responseFail(session, dto, "OPEN_DIRECT_CHAT_FAIL", createOpenDirectChatFailMessage(e));
		}
	}

	private String createOpenDirectChatFailMessage(Exception e) {
		if (e instanceof StatusRuntimeException grpcException) {
			String description = grpcException.getStatus().getDescription();

			if (description != null && !description.isBlank()) {
				return description;
			}
		}

		if (e.getMessage() != null && !e.getMessage().isBlank()) {
			return e.getMessage();
		}

		return "1:1 채팅방 열기 실패";
	}

	// ====== 기존 채팅방 입장 ===========================================================================================================
	public void handleEnterRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		RoomIdDTO payload = wsGatePayloadConverter.convert(dto, RoomIdDTO.class);

		if (payload.getRoomId() == null) {
			wsGateOutboundWriter.responseFail(session, dto, "ENTER_ROOM_FAIL", "roomId 없음");
			return;
		}

		EnterRoomCommand enterRomCmd = new EnterRoomCommand(payload.getRoomId(), me.getUserId(), me.getPublicId());

		EnterRoomResponseDTO enterRoomResponse = wsGateRoomClient.enterRoom(enterRomCmd);

		wsGateSessionRegistry.enterRoomSession(enterRoomResponse.getRoomId(), me.getUserId(), session);
		log.info("{}번 유저 {}번방 입장. wsSess등록.", me.getUserId(), payload.getRoomId());

		wsGateOutboundWriter.responseOk(session, dto, "ENTER_ROOM_OK", enterRoomResponse);
	} // handleEnterRoom

	// ====== 채팅방 닫기 ===========================================================================================================
	// ** 단순히 wsRoomSession에서만 제거하므로 engine 불필요.
	public void handleExitRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadExitRoomRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadExitRoomRequestDTO.class);

		if (payload.getRoomId() == null) {
			wsGateOutboundWriter.responseFail(session, dto, "EXIT_ROOM_FAIL", "roomId,userId가 없습니다.");
			return;
		}

		wsGateSessionRegistry.exitRoomSession(payload.getRoomId(), me.getUserId());

		log.info("{}번 유저 {}번방 Exit 처리", me.getUserId(), payload.getRoomId());
		wsGateOutboundWriter.responseOk(session, dto, "EXIT_ROOM_OK", payload);
	}//exitRoom 끝.

	//	====== 채팅방 나가기 ===========================================================================================================
	public void handleLeftRoom(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		RoomIdDTO payload = wsGatePayloadConverter.convert(dto, RoomIdDTO.class);

		if (payload.getRoomId() == null) {
			wsGateOutboundWriter.responseFail(session, dto, "LEFT_ROOM_FAIL", "roomId 없음");
			return;
		}

		LeftRoomCommand leftRomCmd = new LeftRoomCommand(payload.getRoomId(), me.getUserId(), me.getPublicId());

		RoomFeedResponseDTO response = wsGateRoomClient.leftRoom(leftRomCmd);

		wsGateSessionRegistry.exitRoomSession(payload.getRoomId(), me.getUserId());
		wsGateOutboundWriter.broadcastToRoom(payload.getRoomId(), "LEFT_ROOM", response, dto.getRequestId());
		wsGateOutboundWriter.responseOk(session, dto, "LEFT_ROOM_OK", response);
	}

	//	====== 기존 단톡방에서 멤버 초대 ===========================================================================================================
	public void handleInviteMember(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadInviteMemberRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadInviteMemberRequestDTO.class);

		if (payload.getRoomId() == null || payload.getInviteTargetMemberPublicIds() == null
				|| payload.getInviteTargetMemberPublicIds().isEmpty()) {
			wsGateOutboundWriter.responseFail(session, dto, "INVITE_MEMBER_FAIL", "초대 정보 없음");
			return;
		}

		InviteMemberCommand ivtMbrCmd = new InviteMemberCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload
				.getInviteTargetMemberPublicIds());

		RoomFeedResponseDTO response = wsGateRoomClient.inviteMember(ivtMbrCmd);

		wsGateOutboundWriter.broadcastToRoom(payload.getRoomId(), "ROOM_MEMBER_INVITED", response, dto.getRequestId());

		for (String targetPublicId : response.getTargetPublicIds()) {
			wsGateOutboundWriter.pushToSingleUserByPublicId(targetPublicId, "ROOM_INVITED", response, dto.getRequestId());
		}
	}

	//	====== 단톡방에서 멤버 추방 ===========================================================================================================
	public void handleKickMember(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadKickMemberRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadKickMemberRequestDTO.class);

		if (payload.getRoomId() == null || payload.getKickTargetPublicId() == null || payload.getKickTargetPublicId().isBlank()) {
			wsGateOutboundWriter.responseFail(session, dto, "KICK_MEMBER_FAIL", "강퇴 대상 없음");
			return;
		}

		KickMemberCommand kickMbrCmd = new KickMemberCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload
				.getKickTargetPublicId());

		RoomFeedResponseDTO response = wsGateRoomClient.kickMember(kickMbrCmd);

		for (String targetPublicId : response.getTargetPublicIds()) {
			wsGateSessionRegistry.exitRoomSessionByPublicId(payload.getRoomId(), targetPublicId);
		}

		wsGateOutboundWriter.broadcastToRoom(payload.getRoomId(), "ROOM_MEMBER_KICKED", response, dto.getRequestId());

		for (String targetPublicId : response.getTargetPublicIds()) {
			wsGateOutboundWriter.pushToSingleUserByPublicId(targetPublicId, "ROOM_KICKED", response, dto.getRequestId());
		}
	}

	//	====== 단톡방에서 영구 강퇴 ===========================================================================================================
	public void handleBanMember(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadBanMemberRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadBanMemberRequestDTO.class);

		if (payload.getRoomId() == null || payload.getBanTargetPublicId() == null || payload.getBanTargetPublicId().isBlank()) {
			wsGateOutboundWriter.responseFail(session, dto, "BAN_MEMBER_FAIL", "밴 대상 없음");
			return;
		}

		BanMemberCommand banMbrCmd = new BanMemberCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload.getBanTargetPublicId());

		RoomFeedResponseDTO response = wsGateRoomClient.banMember(banMbrCmd);

		wsGateOutboundWriter.broadcastToRoom(payload.getRoomId(), "ROOM_MEMBER_BANNED", response, dto.getRequestId());
	}

	//	====== 기존 단톡방에서 멤버 초대하기 ===========================================================================================================
	public void handleChangeMemberRole(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadChangeMemberRoleRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadChangeMemberRoleRequestDTO.class);

		if (payload.getRoomId() == null || payload.getTargetPublicId() == null || payload.getTargetRole() == null) {
			wsGateOutboundWriter.responseFail(session, dto, "CHANGE_MEMBER_ROLE_FAIL", "권한 변경 정보 없음");
			return;
		}

		ChangeMemberRoleCommand chgMbrRolCmd = new ChangeMemberRoleCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload
				.getTargetPublicId(), payload.getTargetRole());

		RoomFeedResponseDTO response = wsGateRoomClient.changeMemberRole(chgMbrRolCmd);

		wsGateOutboundWriter.broadcastToRoom(payload.getRoomId(), "ROOM_MEMBER_ROLE_CHANGED", response, dto.getRequestId());
	}

	//	====== 방 공지사항 등록/수정/내림/재등록/삭제 ===========================================================================================================
	public void handleApplyRoomNotice(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadApplyRoomNoticeRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadApplyRoomNoticeRequestDTO.class);

		if (payload.getRoomId() == null || payload.getRoomNoticeAction() == null || payload.getRoomNoticeAction().isBlank()) {
			log.warn("APPLY_ROOM_NOTICE 누락 : roomId:{} act:{} type:{} ctn:{}", payload.getRoomId(), payload.getRoomNoticeAction(), payload
					.getRoomNoticeType(), payload.getRoomNoticeContents());
			wsGateOutboundWriter.responseFail(session, dto, "APPLY_ROOM_NOTICE_FAIL", "roomId 또는 action 누락");
			return;
		}

		try {
			ApplyRoomNoticeCommand applyRomNotiCmd = new ApplyRoomNoticeCommand(payload.getRoomId(), payload.getRoomNoticeAction(), payload
					.getTargetRoomNoticeId(), payload
							.getRoomNoticeType(), payload.getSourceMessageId(), payload.getRoomNoticeContents(), me.getUserId(), me.getPublicId());

			RoomNoticeApplyResponseDTO grpcResponse = wsGateRoomClient.applyRoomNotice(applyRomNotiCmd);

			wsGateOutboundWriter
					.broadcastToRoom(grpcResponse.getRoomNoticeView().getRoomId(), "ROOM_NOTICE_APPLIED", grpcResponse, dto.getRequestId());

		} catch (Exception e) {
			log.error("ROOM_NOTICE_APPLIED 예외처리발생", e);
			wsGateOutboundWriter.responseFail(session, dto, "ROOM_NOTICE_APPLIED_FAIL", e.getMessage() == null ? "ROOM_NOTICE_APPLIED 예외처리발생" : e.getMessage());
		}

	}

}// WsGateRoomHandler 끝.
