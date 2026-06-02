package com.chat.castledragon.domain;

import lombok.Data;

@Data
public class PayloadSendMessageDTO {
	private Long roomId;
	private String msgText;
}

// private Long senderId;
// private String senderLoginId;
// session에서 꺼내먹을거니까 굳이 필요없음.