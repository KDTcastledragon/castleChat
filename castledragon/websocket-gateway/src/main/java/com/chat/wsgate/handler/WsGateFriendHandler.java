package com.chat.wsgate.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.friend.command.AddFriendCommand;
import com.chat.contract.friend.command.RespondFriendCommand;
import com.chat.contract.friend.domain.res.FriendEventResponseDTO;
import com.chat.contract.user.domain.SessionUserDTO;
import com.chat.contract.websocket.domain.WebSocketDTO;
import com.chat.wsgate.auth.WsGateAuth;
import com.chat.wsgate.client.WsGateFriendClient;
import com.chat.wsgate.domain.friend.PayloadAddFriendRequestDTO;
import com.chat.wsgate.domain.friend.PayloadRespondFriendRequestDTO;
import com.chat.wsgate.outbound.WsGateOutboundWriter;
import com.chat.wsgate.support.WsGatePayloadConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class WsGateFriendHandler {

	private final WsGateAuth wsGateAuth;
	private final WsGatePayloadConverter wsGatePayloadConverter;
	private final WsGateFriendClient wsGateFriendClient;
	private final WsGateOutboundWriter wsGateOutboundWriter;

	public void handleAddFriend(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadAddFriendRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadAddFriendRequestDTO.class);

		if (payload.getTargetPublicId() == null || payload.getTargetPublicId().isBlank()) {
			wsGateOutboundWriter.responseFail(session, dto, "ADD_FRIEND_FAIL", "targetPublicId 없음");
			return;
		}

		try {
			AddFriendCommand addFriCmd = new AddFriendCommand(me.getUserId(), me.getPublicId(), payload.getTargetPublicId());

			FriendEventResponseDTO response = wsGateFriendClient.addFriend(addFriCmd);

			wsGateOutboundWriter.responseOk(session, dto, "ADD_FRIEND_OK", response);
			wsGateOutboundWriter.dispatchToUser(response.getTargetUserId(), "FRIEND_REQUEST_RECEIVED", response, dto.getRequestId());

		} catch (Exception e) {
			log.error("ADD_FRIEND 예외", e);
			wsGateOutboundWriter.responseFail(session, dto, "ADD_FRIEND_FAIL", "친구 요청 실패");
		}
	}

	public void handleRespondFriend(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadRespondFriendRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadRespondFriendRequestDTO.class);

		if (payload.getRequesterPublicId() == null || payload.getRequesterPublicId().isBlank() || payload.getFriendAction() == null
				|| payload.getFriendAction().isBlank()) {
			wsGateOutboundWriter.responseFail(session, dto, "RESPOND_FRIEND_FAIL", "친구 응답 정보 없음");
			return;
		}

		try {
			RespondFriendCommand rspFriCmd = new RespondFriendCommand(me.getUserId(), me.getPublicId(), payload.getRequesterPublicId(), payload
					.getFriendAction());

			FriendEventResponseDTO response = wsGateFriendClient.respondFriend(rspFriCmd);

			wsGateOutboundWriter.responseOk(session, dto, "RESPOND_FRIEND_OK", response);
			wsGateOutboundWriter.dispatchToUser(response.getRequesterUserId(), "FRIEND_REQUEST_RESPONDED", response, dto.getRequestId());

		} catch (Exception e) {
			log.error("RESPOND_FRIEND 예외", e);
			wsGateOutboundWriter.responseFail(session, dto, "RESPOND_FRIEND_FAIL", "친구 요청 응답 실패");
		}
	}
}