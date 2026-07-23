package com.chat.aiassist.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefineMessageToneRequestDTO {
	private String messageText;
	private String tone;
}
