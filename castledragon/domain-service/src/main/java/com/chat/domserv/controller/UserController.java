package com.chat.domserv.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chat.castledragon.domain.LoginRequestDTO;
import com.chat.castledragon.domain.SessionUserDTO;
import com.chat.castledragon.domain.UserDTO;
import com.chat.castledragon.domain.UserProfileResponseDTO;
import com.chat.castledragon.service.UserService;
import com.chat.castledragon.websocket.WsDispatcher;

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

	WsDispatcher wsDispatcher; // private final을 꼭 붙여야 하나??

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginData, HttpSession session) {
		log.info("{}의 login 시도  :", loginData.getLoginId());

		UserDTO user = userService.login(loginData.getLoginId(), loginData.getPassword());

		if (user == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("존재하지 않는 사용자.");
		}

		SessionUserDTO sessionUser = new SessionUserDTO(user.getUserId(), user.getPublicId(), user.getNickname(), user.getFriendCode(), user.getProfileImg());

		session.setAttribute("LOGIN_USER", sessionUser);

		UserProfileResponseDTO loginResponse = new UserProfileResponseDTO(user.getPublicId(), user.getNickname(), user.getFriendCode(), user.getProfileImg());

		return ResponseEntity.ok(loginResponse);
	}// login

	@GetMapping("/isMe")
	public ResponseEntity<?> isMe(HttpSession session) {
		SessionUserDTO loginUser = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		UserProfileResponseDTO isMeTrue = new UserProfileResponseDTO(loginUser.getPublicId(), loginUser.getNickname(), loginUser.getFriendCode(), loginUser.getProfileImg()); // friCode = null

		return ResponseEntity.ok(isMeTrue);
	}// isMe

	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session) {

		SessionUserDTO loginUser = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (loginUser != null) {
			wsDispatcher.closeUserWebSocketConnection(loginUser.getUserId());
		}

		session.invalidate();

		return ResponseEntity.ok().build();
	}// logout

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
	}// logIdDupCheck

	// ====[회원가입]========================================================================================
	@PostMapping("/join")
	public ResponseEntity<?> join(@RequestBody UserDTO data) {
		try {
			log.info("");
			log.info("join data : {} {} {}", data.getLoginId(), data.getPassword(), data.getNickname());

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
	@GetMapping("/searchUsers")
	public ResponseEntity<?> searchUsers(@RequestParam("searchWord") String searchWord, HttpSession session) {
		SessionUserDTO me = (SessionUserDTO) session.getAttribute("LOGIN_USER"); // 여기서 이미 현재 검색한 사람이 누구인지 나와.

		if (me == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		log.info("{} 유저가 검색중 : {}", me.getNickname(), searchWord);

		if (searchWord == null || searchWord.trim().isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		List<UserProfileResponseDTO> searchedList = userService.searchUsers(searchWord.trim(), me.getUserId());

		log.info("{} 유저의 {} 검색결과 : {}", me.getNickname(), searchWord, searchedList);

		return ResponseEntity.ok(searchedList);
	}

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
