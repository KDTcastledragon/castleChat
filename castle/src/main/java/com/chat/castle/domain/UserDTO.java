package com.chat.castle.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserDTO {
	private String user_id;
	private String user_pw;
	private String user_name;
	private String phone_num;
	private LocalDate birth;
	private String thumbnail;
	private LocalDateTime join_date;
	private LocalDateTime latest_updated;
	private LocalDateTime withdrawal_date;
}
