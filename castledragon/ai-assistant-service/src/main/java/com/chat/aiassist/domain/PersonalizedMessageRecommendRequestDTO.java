package com.chat.aiassist.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedMessageRecommendRequestDTO {
	private Long roomId;
	private String targetPublicId;
	private String relationshipType;
}
