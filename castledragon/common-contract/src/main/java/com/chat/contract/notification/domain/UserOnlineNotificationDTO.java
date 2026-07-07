package com.chat.contract.notification.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOnlineNotificationDTO {
	private Long userId;
	private String publicId;
	private String nickname;
	private String profileImg;

	private String notificationText;
	private LocalDateTime notifiedAt;
}
