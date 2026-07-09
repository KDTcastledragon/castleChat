package com.chat.chengine.worker;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chengine.config.ChatKafkaConfig;
import com.chat.chengine.dto.ChatMessageCreatedEventDTO;
import com.chat.chengine.mapper.ChatMapper;
import com.chat.contract.chatting.domain.ChatMessagesDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * sendMessage의 DB insert 비동기 워커.
 *
 * prpr 3 : DB insert는 kafka에서 비동기로 묶어서 처리한다.
 * client는 이미 durable save 시점에 응답을 받았고, 여기서는 배치성으로 뒤따라 insert만 한다.
 *
 * 멱등성 : messageId가 발행 전에 이미 확정돼 있고 insert는 INSERT IGNORE 라서
 * 재전달(redelivery)이 와도 중복 insert 되지 않는다.
 * 실패 시 : ChatKafkaConfig의 에러핸들러가 0.5초 x 10회 재시도 후 DLT로 보낸다.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class ChatMessagePersistWorker {

	private final ChatMapper chatMapper;

	@KafkaListener(topics = ChatKafkaConfig.CHAT_MESSAGE_CREATED_TOPIC, groupId = "${chat.kafka.persist-group-id:chengine-chat-persist}")
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
}
