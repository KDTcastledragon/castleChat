package com.chat.domserv.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageReaderCandidateDTO {
	private Long userId;
	private String publicId;
	private String nickname;
	private String friendCode;
	private String profileImg;
	private String role;
	private Long lastReadMessageId;
}
