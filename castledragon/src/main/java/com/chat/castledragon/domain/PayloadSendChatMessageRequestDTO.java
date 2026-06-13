package com.chat.castledragon.domain;

import lombok.Data;

@Data
public class PayloadSendChatMessageRequestDTO {
	private Long roomId;
	private String messageText;
}

// private Long senderId;
// private String senderLoginId;
// session에서 꺼내먹을거니까 굳이 필요없음.