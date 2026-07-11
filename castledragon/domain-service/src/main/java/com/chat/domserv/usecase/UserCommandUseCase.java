package com.chat.domserv.usecase;

import com.chat.domserv.domain.UserDTO;

public interface UserCommandUseCase {
	boolean join(String loginId, String password, String nickname, String profileImg);

	boolean changePassWord(String id, String newPw);

	boolean withdrawMember(String id);

	UserDTO login(String id, String pw);
}
