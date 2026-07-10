// chat kafka 이벤트를 소비해서 db write를 전담한다.
package com.chat.evtwk.worker;

import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chat.contract.chatting.command.ReactChatMessageCommand;
import com.chat.contract.chatting.domain.ChatMessagesDTO;
import com.chat.contract.event.chatting.ChatMessageCreatedEventDTO;
import com.chat.contract.event.chatting.ChatMessageDeletedEventDTO;
import com.chat.contract.event.chatting.ChatMessageReactedEventDTO;
import com.chat.contract.kafka.ChatKafkaTopics;
import com.chat.evtwk.mapper.EventPersistChatMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
@KafkaListener(topics = ChatKafkaTopics.CHAT_EVENT_TOPIC, groupId = "${chat.kafka.persist-group-id:event-persist-worker}")
public class ChatEventPersistWorker {

	private final EventPersistChatMapper chatMapper;

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
			log.info("[persistMsg]이미 저장된 메시지. msgId={}, room={}", event.getMessageId(), event.getRoomId());
			return;
		}

		if (event.getAttachmentIds() != null && !event.getAttachmentIds().isEmpty()) {
			int attached = chatMapper.updateChatMessageAttachments(event.getMessageId(), event.getRoomId(), event.getAttachmentIds());

			if (attached != event.getAttachmentIds().size()) {
				log.warn("[persistMsg]첨부 연결 개수 불일치. msgId={}, room={}, 요청={}, 처리={}", event.getMessageId(), event
						.getRoomId(), event.getAttachmentIds().size(), attached);
			}
		}

		log.info("[persistMsg]db insert 완료. msgId={}, room={}", event.getMessageId(), event.getRoomId());
	}

	@KafkaHandler
	@Transactional
	public void persistDeleteChatMessage(ChatMessageDeletedEventDTO event) {
		int updated = chatMapper.deleteChatMessage(event.getRoomId(), event.getMessageId(), event.getRequesterUserId());

		if (updated == 0) {
			log.info("[persistDeleteMsg]이미 삭제된 메시지. msgId={}, room={}", event.getMessageId(), event.getRoomId());
			return;
		}

		log.info("[persistDeleteMsg]db update 완료. msgId={}, room={}", event.getMessageId(), event.getRoomId());
	}

	@KafkaHandler
	@Transactional
	public void persistReactChatMessage(ChatMessageReactedEventDTO event) {
		if (Boolean.TRUE.equals(event.getAddRequested())) {
			ReactChatMessageCommand cmd = new ReactChatMessageCommand(event.getRoomId(), event.getMessageId(), event
					.getRequesterUserId(), event.getRequesterPublicId(), event.getReactionType(), event.getReactionCode(), true);

			int inserted = chatMapper.insertChatMessageReaction(cmd);

			if (inserted == 0) {
				log.info("[persistReactMsg]이미 반영된 리액션. msgId={}, room={}, code={}", event.getMessageId(), event
						.getRoomId(), event.getReactionCode());
				return;
			}

			log.info("[persistReactMsg]리액션 insert 완료. msgId={}, room={}, code={}", event.getMessageId(), event
					.getRoomId(), event.getReactionCode());
			return;
		}

		int deleted = chatMapper.deleteChatMessageReaction(event.getRoomId(), event.getMessageId(), event.getRequesterUserId(), event
				.getReactionCode());

		if (deleted == 0) {
			log.info("[persistReactMsg]이미 취소된 리액션. msgId={}, room={}, code={}", event.getMessageId(), event
					.getRoomId(), event.getReactionCode());
			return;
		}

		log.info("[persistReactMsg]리액션 delete 완료. msgId={}, room={}, code={}", event.getMessageId(), event.getRoomId(), event
				.getReactionCode());
	}
}
