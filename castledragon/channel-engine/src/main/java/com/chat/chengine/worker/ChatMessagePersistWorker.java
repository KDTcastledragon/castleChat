package com.chat.chengine.worker;

import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chengine.config.ChatKafkaConfig;
import com.chat.chengine.domain.ChatMessageCreatedEventDTO;
import com.chat.chengine.domain.ChatMessageDeletedEventDTO;
import com.chat.chengine.domain.ChatMessageReactedEventDTO;
import com.chat.chengine.mapper.ChatMapper;
import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.domain.ChatMessagesDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 메시지 도메인 이벤트(created/deleted/reacted)의 DB CRUD 비동기 워커.
 *
 * prpr 3 : DB CRUD는 kafka에서 비동기로 묶어서 처리한다.
 * client는 이미 durable save 시점에 응답을 받았고, 여기서는 뒤따라 DB 반영만 한다.
 *
 * 이벤트 타입 라우팅 : producer(JsonSerializer)가 심는 __TypeId__ 헤더를 보고
 * spring-kafka가 타입에 맞는 @KafkaHandler 메소드로 자동 분배한다.
 *
 * 멱등성 : created=INSERT IGNORE / deleted=조건부 UPDATE(status='ACTIVE'만) / reacted=INSERT IGNORE·DELETE
 * -> 재전달(redelivery)이 와도 중복 반영 없음.
 * 실패 시 : ChatKafkaConfig의 에러핸들러가 0.5초 x 10회 재시도 후 DLT로 보낸다.
 */
@Component
@Log4j2
@RequiredArgsConstructor
@KafkaListener(topics = ChatKafkaConfig.CHAT_MESSAGE_TOPIC, groupId = "${chat.kafka.persist-group-id:chengine-chat-persist}")
public class ChatMessagePersistWorker {

	private final ChatMapper chatMapper;

	@KafkaHandler
	@Transactional
	public void persistChatMessage(ChatMessageCreatedEventDTO event) {
		ChatMessagesDTO msg = new ChatMessagesDTO();
		msg.setMessageId(event.getMessageId());
		msg.setRoomId(event.getRoomId());
		msg.setSenderId(event.getSenderUserId());
		msg.setMessageType(event.getMessageType());
		msg.setMessageText(event.getMessageText());
		msg.setReplyToMessageId(event.getReplyToMessageId());
		msg.setCreatedAt(event.getCreatedAt());

		int inserted = chatMapper.insertChatMessage(msg);

		if (inserted == 0) {
			// INSERT IGNORE + 동일 messageId = 이미 처리된 이벤트(재전달). 멱등 처리로 정상 종료.
			log.info("[persistMsg]이미 저장된 메시지. 재전달 무시. msgId={}, room={}", event.getMessageId(), event.getRoomId());
			return;
		}

		if (event.getAttachmentIds() != null && !event.getAttachmentIds().isEmpty()) {
			int attached = chatMapper.updateChatMessageAttachments(event.getMessageId(), event.getRoomId(), event.getAttachmentIds());

			if (attached != event.getAttachmentIds().size()) {
				// TEMP가 아닌(이미 ATTACHED 등) row가 섞이면 개수가 안 맞을 수 있다. 재시도해도 결과가 같으므로 DLT로 보내지 않고 경고만 남긴다.
				log.warn("[persistMsg]첨부 연결 개수 불일치. msgId={}, room={}, 요청={}, 처리={}", event.getMessageId(), event
						.getRoomId(), event.getAttachmentIds().size(), attached);
			}
		}

		log.info("[persistMsg]DB insert 완료. msgId={}, room={}", event.getMessageId(), event.getRoomId());
	}

	@KafkaHandler
	@Transactional
	public void persistDeleteChatMessage(ChatMessageDeletedEventDTO event) {
		// 작성자/상태 검증은 발행 전 ChatCommandService에서 동기로 끝났다. 여기서는 조건부 UPDATE만.
		int updated = chatMapper.deleteChatMessage(event.getRoomId(), event.getMessageId(), event.getRequesterUserId());

		if (updated == 0) {
			// 이미 삭제됐거나(재전달) 동시 삭제 경합. WHERE status='ACTIVE' 조건이 걸러줬으므로 멱등 처리로 정상 종료.
			log.info("[persistDeleteMsg]이미 삭제된 메시지. 재전달/경합 무시. msgId={}, room={}", event.getMessageId(), event.getRoomId());
			return;
		}

		log.info("[persistDeleteMsg]DB update 완료. msgId={}, room={}", event.getMessageId(), event.getRoomId());
	}

	@KafkaHandler
	@Transactional
	public void persistReactChatMessage(ChatMessageReactedEventDTO event) {
		if (Boolean.TRUE.equals(event.getAddRequested())) {
			ReactChatMessageCommand cmd = new ReactChatMessageCommand(event.getRoomId(), event.getMessageId(), event
					.getRequesterUserId(), event.getRequesterPublicId(), event.getReactionType(), event.getReactionCode(), true);

			int inserted = chatMapper.insertChatMessageReaction(cmd);

			if (inserted == 0) {
				// INSERT IGNORE + 동일 (room,msg,user,code) = 이미 반영된 리액션(재전달). 멱등 처리.
				log.info("[persistReactMsg]이미 반영된 리액션. 재전달 무시. msgId={}, room={}, code={}", event.getMessageId(), event
						.getRoomId(), event.getReactionCode());
			} else {
				log.info("[persistReactMsg]리액션 insert 완료. msgId={}, room={}, code={}", event.getMessageId(), event
						.getRoomId(), event.getReactionCode());
			}

			return;
		}

		int deleted = chatMapper.deleteChatMessageReaction(event.getRoomId(), event.getMessageId(), event.getRequesterUserId(), event
				.getReactionCode());

		if (deleted == 0) {
			// 이미 취소됐거나 없는 리액션(재전달). 멱등 처리.
			log.info("[persistReactMsg]이미 취소된 리액션. 재전달 무시. msgId={}, room={}, code={}", event.getMessageId(), event
					.getRoomId(), event.getReactionCode());
			return;
		}

		log.info("[persistReactMsg]리액션 delete 완료. msgId={}, room={}, code={}", event.getMessageId(), event.getRoomId(), event
				.getReactionCode());
	}
}
