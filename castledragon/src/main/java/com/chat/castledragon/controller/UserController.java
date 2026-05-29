package com.chat.castledragon.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.castledragon.domain.UserDTO;
import com.chat.castledragon.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
@Log4j2
public class UserController {
	UserService userService;

	PasswordEncoder pwEncoder;

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, Object> data, HttpSession session) {
		log.info("loginData :" + data);
		String loginId = (String) data.get("loginId");
		String password = (String) data.get("password");

		UserDTO user = userService.login(loginId, password);

		if (user == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("존재하지 않는 사용자.");
		}

		session.setAttribute("LOGIN_USER", user);

		return ResponseEntity.ok(user);
	}// login

	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session) {
		session.invalidate();
		return ResponseEntity.ok().build();
	}

	// ====[중복체크]========================================================================================
	@PostMapping("/loginIdDuplicateCheck")
	public ResponseEntity<?> idDupCheck(@RequestBody UserDTO data) {
		try {
			log.info("");

			String loginId = data.getLoginId() != null ? data.getLoginId() : null;

			boolean isDuplicated = userService.loginIdDuplicateCheck(loginId);

			if (isDuplicated) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("conflict");
			} else {
				return ResponseEntity.ok().build();
			}
		} catch (Exception e) {
			throw e;
		}
	}// loginIdDuplicateCheck

	private String createFriendCode() {
		int number = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 100_000_000);

		return String.format("#%08d", number);
	}

	// ====[회원가입]========================================================================================
	@PostMapping("/join")
	public ResponseEntity<?> join(@RequestBody UserDTO data) {
		try {
			log.info("");
			log.info("join data : {}", data);

			String loginId = data.getLoginId();
			String password = data.getPassword();
			String nickname = data.getNickname();

			if (loginId == null || password == null || nickname == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("필수값 누락");
			}

			log.info("회원가입 : {} {} {} {} {}", loginId, password, nickname);// 추후 삭제.

			boolean isJoined = userService.join(loginId, password, nickname);

			log.info("뭐가문제냐구웅웅웅 : {}", isJoined);

			if (isJoined == true) {
				return ResponseEntity.ok().build();

			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("service 오류");
			}

		} catch (Exception e) {
			throw e;
		}
	}// join

	//====[비밀번호 변경]==========================================
	@PostMapping("/changePassword")
	public ResponseEntity<?> changePassword(@RequestBody Map<String, String> pwData) {
		try {
			String id = pwData.get("id");
			String prevPw = pwData.get("prevPw");
			String newPw = pwData.get("newPw");

			UserDTO dto = userService.getUser(id);

			log.info("비밀번호 일치? {} {}", prevPw, dto.getPassword());

			if (pwEncoder.matches(prevPw, dto.getPassword())) {

				boolean isChanged = userService.changePassWord(id, newPw);
				log.info("비밀번호 변경됨? {}", isChanged);

				return ResponseEntity.ok().body(id); // 200

			} else {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("no match pw"); // 409
			}

		} catch (Exception e) {
			throw e;
		}
	}//changePassword

	@GetMapping("/allUsers")
	public ResponseEntity<?> allUsers() {
		//		log.info("allUsers");
		List<UserDTO> list = userService.allUsers();

		return ResponseEntity.ok(list);
	}

}// chgPw
