package com.chat.wsgate.handler;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.chat.contract.chatting.command.CreateChatMessageCommand;
import com.chat.contract.chatting.command.DeleteChatMessageCommand;
import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.command.ReadChatMessageCommand;
import com.chat.contract.chatting.command.StartDirectChatCommand;
import com.chat.contract.chatting.command.StartGroupChatCommand;
import com.chat.contract.chatting.domain.res.ChatMessageViewResponseDTO;
import com.chat.contract.chatting.domain.res.DeleteChatMessageResponseDTO;
import com.chat.contract.chatting.domain.res.ReactChatMessageEventResponseDTO;
import com.chat.contract.chatting.domain.res.ReadPositionUpdateResponseDTO;
import com.chat.contract.user.domain.SessionUserDTO;
import com.chat.contract.websocket.domain.WebSocketDTO;
import com.chat.wsgate.auth.WsGateAuth;
import com.chat.wsgate.client.WsGateChatClient;
import com.chat.wsgate.domain.chatting.PayloadDeleteChatMessageRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadReactChatMessageRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadReadChatMessageRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadSendChatMessageRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadStartDirectChatRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadStartGroupChatRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadTypingRequestDTO;
import com.chat.wsgate.domain.chatting.PayloadTypingResponseDTO;
import com.chat.wsgate.outbound.WsGateOutboundWriter;
import com.chat.wsgate.session.WsGateSessionRegistry;
import com.chat.wsgate.support.WsGatePayloadConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class WsGateChatHandler {
	private final WsGateAuth wsGateAuth;
	private final WsGateOutboundWriter wsGateOutboundWriter;
	private final WsGatePayloadConverter wsGatePayloadConverter;
	private final WsGateChatClient wsGateChatClient;
	private final WsGateSessionRegistry wsGateSessionRegistry;

	private boolean isEmptyMessage(String messageText, List<Long> attachmentIds) {
		boolean noText = messageText == null || messageText.isBlank();
		boolean noAttachment = attachmentIds == null || attachmentIds.isEmpty();

		return noText && noAttachment;
	}

	private String defaultMessageType(String messageType) {
		if (messageType == null || messageType.isBlank()) {
			return "TEXT";
		}

		return messageType;
	}

	//	====== 채팅 입력 이벤트 start/stop =========================================================================================================
	//*** 이 메소드만 예외적으로 channel-engine을 거치지 않고, 그대로 response 한다. business logic이 없기 때문. [ 책임분리 < UX ] 
	public void handleTyping(WebSocketSession session, WebSocketDTO dto, String eventType) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadTypingRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadTypingRequestDTO.class);

		if (payload.getRoomId() == null) {
			log.warn("타이핑 데이터 중 roomId null");
			wsGateOutboundWriter.responseFail(session, dto, eventType + "_FAIL", "TYPING 필수값 누락");
			return;
		}

		PayloadTypingResponseDTO responsePayload = new PayloadTypingResponseDTO(payload.getRoomId(), me.getPublicId(), me.getNickname());

		//		log.info("{} in room={}", eventType, responsePayload.getRoomId());

		wsGateOutboundWriter.broadcastToRoomExceptUser(payload.getRoomId(), eventType, responsePayload, dto.getRequestId(), me.getUserId());
	}

	// 1.로그인 검증  2.payload 꺼내기  3.data누락 검증  4.커맨드 생성  5.grpc 커맨드 input + grpc output 받기  6.output broadcast 

	//	====== 1:1 채팅방 메세지 전송 후 방 생성 ======================================================================================================
	public void handleStartDirectChat(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadStartDirectChatRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadStartDirectChatRequestDTO.class);

		if (payload.getTargetPublicId() == null || payload.getTargetPublicId().isBlank()) {
			wsGateOutboundWriter.responseFail(session, dto, "START_DIRECT_ROOM_WITH_MSG_FAIL", "targetPublicId 누락");
			return;
		}

		if (isEmptyMessage(payload.getMessageText(), payload.getAttachmentIds())) {
			wsGateOutboundWriter.responseFail(session, dto, "START_DIRECT_ROOM_WITH_MSG_FAIL", "메시지 내용 없음");
			return;
		}

		try {
			StartDirectChatCommand startDirChtCmd = new StartDirectChatCommand(payload.getTargetPublicId(), me.getUserId(), me
					.getPublicId(), defaultMessageType(payload.getMessageType()), payload
							.getMessageText(), payload.getReplyToMessageId(), payload.getAttachmentIds());

			ChatMessageViewResponseDTO grpcResponse = wsGateChatClient.startDirectChat(startDirChtCmd);

			wsGateSessionRegistry.enterRoomSession(grpcResponse.getRoomId(), me.getUserId(), session);
			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_CREATED", grpcResponse, dto.getRequestId());
			log.info("{}번유저 -> {}번방 start갠톡 : {}", me.getUserId(), grpcResponse.getRoomId(), grpcResponse.getMessageText());

		} catch (Exception e) {
			log.error("START_DIRECT_ROOM_WITH_MSG 예외", e);
			wsGateOutboundWriter.responseFail(session, dto, "START_DIRECT_ROOM_WITH_MSG_FAIL", "1:1 첫 메시지 전송 실패");
		}
	}

	//	====== 단톡방 메세지 전송 후 방 생성 ======================================================================================================
	public void handleStartGroupChat(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadStartGroupChatRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadStartGroupChatRequestDTO.class);

		if (payload.getInviteMemberPublicIds() == null || payload.getInviteMemberPublicIds().isEmpty()) {
			wsGateOutboundWriter.responseFail(session, dto, "START_GROUP_ROOM_WITH_MSG_FAIL", "초대 멤버 없음");
			return;
		}

		if (isEmptyMessage(payload.getMessageText(), payload.getAttachmentIds())) {
			wsGateOutboundWriter.responseFail(session, dto, "START_GROUP_ROOM_WITH_MSG_FAIL", "메시지 내용 없음");
			return;
		}

		try {
			StartGroupChatCommand startGrpChtCmd = new StartGroupChatCommand(payload.getRoomName(), payload.getRoomThumbnail(), payload
					.getInviteMemberPublicIds(), me.getUserId(), me.getPublicId(), defaultMessageType(payload.getMessageType()), payload
							.getMessageText(), payload.getReplyToMessageId(), payload.getAttachmentIds());

			ChatMessageViewResponseDTO grpcResponse = wsGateChatClient.startGroupChat(startGrpChtCmd);

			wsGateSessionRegistry.enterRoomSession(grpcResponse.getRoomId(), me.getUserId(), session);
			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_CREATED", grpcResponse, dto.getRequestId());
			log.info("{}번유저 -> {}번방 start단톡 : {}", me.getUserId(), grpcResponse.getRoomId(), grpcResponse.getMessageText());
		} catch (Exception e) {
			log.error("START_GROUP_ROOM_WITH_MSG 예외", e);
			wsGateOutboundWriter.responseFail(session, dto, "START_GROUP_ROOM_WITH_MSG_FAIL", "그룹방 첫 메시지 전송 실패");
		}
	}

	//	====== 메세지 전송 ===========================================================================================================
	public void handleSendMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadSendChatMessageRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadSendChatMessageRequestDTO.class);

		if (payload.getRoomId() == null) {
			wsGateOutboundWriter.responseFail(session, dto, "SEND_MSG_FAIL", "roomId 누락");
			return;
		}

		if (isEmptyMessage(payload.getMessageText(), payload.getAttachmentIds())) {
			wsGateOutboundWriter.responseFail(session, dto, "SEND_MSG_FAIL", "메시지 내용 없음");
			return;
		}// null 검사하는 이 행위는 비즈니스 로직이라기보다 transport/request shape 검증. ---> payload가 명령으로 성립하는 최소 형태인가?
			// handler 검증 = 이 요청을 command로 만들 수 있는가? --> protocol boundary.
			// “깨진 패킷을 굳이 내부 서비스까지 보내지 않기 위한 입구 필터” 역할을 위해 payload null값 검증.

		try {
			// Command 생성. 최적화를 위해 me에서 publicId까지 꺼내고 함께 보낸다. orc에서 userId로 publicId를 DB에서 굳이 또 하는 것보다 좋음. session값이라 신뢰 가능.
			CreateChatMessageCommand createChtMsgCmd = new CreateChatMessageCommand(payload.getRoomId(), me.getUserId(), me
					.getPublicId(), defaultMessageType(payload.getMessageType()), payload
							.getMessageText(), payload.getReplyToMessageId(), payload.getAttachmentIds());

			ChatMessageViewResponseDTO grpcResponse = wsGateChatClient.createChatMessage(createChtMsgCmd);

			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_CREATED", grpcResponse, dto.getRequestId()); // chatService.sendMessage()가 성공했을 때만 broadcast해야 하니까. try{}안에 두어라.
			log.info("{}번유저 -> {}번방 sendMsg : {}", me.getUserId(), grpcResponse.getRoomId(), grpcResponse.getMessageText());

		} catch (Exception e) {
			log.error("MSG_CREATED 예외처리발생", e);
			wsGateOutboundWriter.responseFail(session, dto, "MSG_CREATED_FAIL", "MSG_CREATED 예외처리발생");
			return;
		}
	}

	//	====== 메세지 읽기 ===========================================================================================================
	public void handleReadMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		//		PayloadReadMessageDTO payload = new PayloadReadMessageDTO(); <--- @NoArgsConstructor가 없어서, 이 부분에서 터져버리는거다. 코드는 딱 1줄이라 티가나질않아서 찾기 빡셈.@NoArgsConstructor 넣고, req res 나누고 코드 쫌만 바꿨는데, 채팅 urc문제 다 해결함;;ㄷ;; 뭐야;
		//		payload.setRoomId(1L);
		//		payload.setLastReadMessageId(6L);
		//		--->
		PayloadReadChatMessageRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadReadChatMessageRequestDTO.class); // ws내부의 payload를 꺼낸다.

		if (payload.getRoomId() == null || payload.getLastReadMessageId() == null) {
			log.warn("readMsg 필수 값 누락 : {} / {}", payload.getRoomId(), payload.getLastReadMessageId());
			wsGateOutboundWriter.responseFail(session, dto, "READ_MSG_FAIL", "READ_MSG 필수값 누락");
			return;
		}

		try {
			ReadChatMessageCommand readChtMsgCmd = new ReadChatMessageCommand(payload.getRoomId(), me.getUserId(), me.getPublicId(), payload
					.getLastReadMessageId());

			ReadPositionUpdateResponseDTO grpcResponse = wsGateChatClient.readChatMessage(readChtMsgCmd);

			Boolean updatedLog4j2 = grpcResponse == null ? null : grpcResponse.getUpdated(); // log 확인용.

			if (!Boolean.TRUE.equals(grpcResponse.getUpdated())) {
				log.info("[false]gate-ChatHandler.읽음처리 응답 updated : {}", updatedLog4j2);
				return;
				//				throw new IllegalArgumentException("nono Boolean false."); // ,조용히 return 시켜야함.
			}

			log.info("[true]gate-ChatHandler.읽음처리 응답 : {}", grpcResponse);

			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_READ", grpcResponse, dto.getRequestId());
		} catch (Exception e) {
			log.error("READ_MSG 예외처리발생: {}", e);
			wsGateOutboundWriter.responseFail(session, dto, "READ_MSG_FAIL", "READ_MSG 예외처리발생");
		}

	}

	// ====== 메시지 삭제 ====================================================================================================================
	public void handleDeleteMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadDeleteChatMessageRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadDeleteChatMessageRequestDTO.class);

		if (payload.getRoomId() == null || payload.getMessageId() == null) {
			log.warn("delete msg roomId:{} msgId:{} 누락", payload.getRoomId(), payload.getMessageId());
			wsGateOutboundWriter.responseFail(session, dto, "DELETE_MSG_FAIL", "roomId,msgId 누락");
			return;
		}

		try {
			DeleteChatMessageCommand deleteChtMsgCmd = new DeleteChatMessageCommand(payload.getRoomId(), payload.getMessageId(), me.getUserId(), me
					.getPublicId());

			DeleteChatMessageResponseDTO grpcResponse = wsGateChatClient.deleteChatMessage(deleteChtMsgCmd);

			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_DELETED", grpcResponse, dto.getRequestId());
		} catch (Exception e) {
			log.error("MSG_DELETED 예외처리발생: {}", e);
			wsGateOutboundWriter.responseFail(session, dto, "MSG_DELETED_FAIL", "MSG_DELETED 예외처리발생");
		}
	}

	// ====== 메시지 리액션 ====================================================================================================================
	public void handleReactMessage(WebSocketSession session, WebSocketDTO dto) throws Exception {
		SessionUserDTO me = wsGateAuth.requireLoginUser(session);

		PayloadReactChatMessageRequestDTO payload = wsGatePayloadConverter.convert(dto, PayloadReactChatMessageRequestDTO.class);

		if (payload.getRoomId() == null || payload.getMessageId() == null || payload.getReactionType() == null || payload.getReactionCode() == null
				|| payload.getAddRequested() == null) {
			log.warn("MSG_REACTION_UPDATED 필수값 누락 : roomId:{} msgId:{} type:{} code:{} addReq:{}", payload.getRoomId(), payload
					.getMessageId(), payload.getReactionType(), payload.getReactionCode(), payload.getAddRequested());
			wsGateOutboundWriter.responseFail(session, dto, "REACT_MSG_FAIL", "주요값 누락.");
			return;
		}

		try {
			ReactChatMessageCommand reactChtMsgCmd = new ReactChatMessageCommand(payload.getRoomId(), payload.getMessageId(), me.getUserId(), me
					.getPublicId(), payload.getReactionType(), payload.getReactionCode(), payload.getAddRequested());

			ReactChatMessageEventResponseDTO grpcResponse = wsGateChatClient.reactChatMessage(reactChtMsgCmd);

			wsGateOutboundWriter.broadcastToRoom(grpcResponse.getRoomId(), "MSG_REACTION_UPDATED", grpcResponse, dto.getRequestId());
		} catch (Exception e) {
			log.error("MSG_REACTION_UPDATED 예외처리발생: {}", e);
			wsGateOutboundWriter.responseFail(session, dto, "MSG_REACTION_UPDATED_FAIL", "MSG_REACTION_UPDATED 예외처리발생");
		}
	}

}
