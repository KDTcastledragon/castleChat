package com.chat.domserv.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chat.domserv.domain.UserDTO;
import com.chat.domserv.mapper.UserMapper;
import com.chat.domserv.usecase.UserCommandUseCase;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UserCommandService implements UserCommandUseCase {
	@Autowired
	UserMapper userMapper;

	@Autowired
	PasswordEncoder encoder;

	@Override
	public boolean join(String loginId, String password, String nickname, String profileImg) {

		String encodedPassword = encoder.encode(password);

		String charsNumAlp = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		for (int attempt = 0; attempt < 10; attempt++) {
			String publicId = com.github.f4b6a3.ulid.UlidCreator.getMonotonicUlid().toString();

			StringBuilder friendCodeBuilder = new StringBuilder("#");

			for (int i = 0; i < 7; i++) {
				int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(charsNumAlp.length());

				friendCodeBuilder.append(charsNumAlp.charAt(index));
			}

			String friendCode = friendCodeBuilder.toString();

			try {
				int joinUser = userMapper.join(publicId, loginId, encodedPassword, nickname, friendCode, profileImg);

				log.info("회원가입 완료 publicId={}, friendCode={}", publicId, friendCode);

				return joinUser > 0;

			} catch (DuplicateKeyException e) {
				log.warn("publicId/friendCode 중복 발생. 재시도 attempt={}, publicId={}, friendCode={}", attempt + 1, publicId, friendCode);
			}// try-catch

		}// for

		throw new IllegalStateException("publicId/friendCode 생성 실패");

	}

	@Override
	public UserDTO login(String loginId, String password) {
		UserDTO user = userMapper.getUserByLoginId(loginId);

		if (user == null) {
			log.info("id 없음");
			return null;
		}

		if (!encoder.matches(password, user.getPassword())) {
			log.info("pw 일치하지않음");
			return null;
		}
		return user;
	}

	@Override
	public boolean changePassWord(String id, String newPw) {
		String encodedNewPassword = encoder.encode(newPw);

		int isChanged = userMapper.changePassword(id, encodedNewPassword);
		return isChanged > 0;
	}

	@Override
	public boolean withdrawMember(String id) {

		int isWithdrawed = userMapper.withdrawMember(id);

		return isWithdrawed > 0;
	}
}
