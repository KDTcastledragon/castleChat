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

import com.chat.contract.user.domain.SessionUserDTO;
import com.chat.contract.user.domain.UserProfileResponseDTO;
import com.chat.domserv.domain.LoginRequestDTO;
import com.chat.domserv.domain.UserDTO;
import com.chat.domserv.usecase.UserCommandUseCase;
import com.chat.domserv.usecase.UserQueryUseCase;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/user")
@Log4j2
@AllArgsConstructor
public class UserController {
	PasswordEncoder pwEncoder; // 추후 Service로 이동.
	UserCommandUseCase usrCmdUseCase;
	UserQueryUseCase usrQryUseCase;

	// ======[ 회원가입 ]===================================================================================================
	@PostMapping("/join")
	//	너는 Controller 메소드를 직접 호출하지 않잖아.
	//	HTTP request가 오면 Spring이 호출함. --> 먼소리고???????? 자세히 공부 ㄱ.
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

			boolean isJoined = usrCmdUseCase.join(loginId, password, nickname);

			log.info("isJoined : {}", isJoined);

			if (isJoined == true) {
				return ResponseEntity.ok().build();

			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("service 오류");
			}

		} catch (Exception e) {
			throw e;
		}
	}// join

	// ======[ 회원가입시, ID 중복체크 ]===================================================================================================
	@PostMapping("/loginIdDuplicateCheck")
	public ResponseEntity<?> idDupCheck(@RequestBody UserDTO data) {
		try {
			log.info("");

			String loginId = data.getLoginId() != null ? data.getLoginId() : null;

			boolean isDuplicated = usrQryUseCase.loginIdDuplicateCheck(loginId);

			if (isDuplicated) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("conflict");
			} else {
				return ResponseEntity.ok().build();
			}
		} catch (Exception e) {
			throw e;
		}
	}// logIdDupCheck

	// ======[ 로그인 ]===================================================================================================
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginData, HttpSession session) {
		log.info("{}의 login 시도  :", loginData.getLoginId());

		UserDTO user = usrCmdUseCase.login(loginData.getLoginId(), loginData.getPassword());

		if (user == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("존재하지 않는 사용자.");
		}

		SessionUserDTO sessionUser = new SessionUserDTO(user.getUserId(), user.getPublicId(), user.getNickname(), user.getFriendCode(), user
				.getProfileImg());

		session.setAttribute("LOGIN_USER", sessionUser);

		UserProfileResponseDTO loginResponse = new UserProfileResponseDTO(user.getPublicId(), user.getNickname(), user.getFriendCode(), user
				.getProfileImg());

		return ResponseEntity.ok(loginResponse);
	}// login

	// ======[ 로그아웃 ]===================================================================================================
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session) {

		SessionUserDTO loginUser = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		//	▶▶▶ Multi Process로 jar들을 모두 분리하여 module화 시켰기 때문에, wsDp를 직접 부르면 안된다. client쪽에서 자발적으로 ws.close()하도록 변경.
		//		if (loginUser != null) {
		//			wsDispatcher.closeUserWebSocketConnection(loginUser.getUserId());
		//		}

		if (loginUser == null) {
			return ResponseEntity.ok().build();
		}

		session.invalidate();

		return ResponseEntity.ok().build();
	}// logout

	// ======[ 현재 로그인 여부 상태 확인 ]===================================================================================================
	// ▶ client의 Front Request를 무조건 신뢰하면 안된다. 항상 Back에서 검증을 해야한다.
	@GetMapping("/isMe")
	public ResponseEntity<?> isMe(HttpSession session) {
		SessionUserDTO loginUser = (SessionUserDTO) session.getAttribute("LOGIN_USER");

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
		}

		UserProfileResponseDTO isMeTrue = new UserProfileResponseDTO(loginUser.getPublicId(), loginUser.getNickname(), loginUser
				.getFriendCode(), loginUser.getProfileImg()); // friCode = null

		return ResponseEntity.ok(isMeTrue);
	}// isMe

	// ======[ 검색어로 유저 검색 (친구 추가를 위한 선기능) ]===================================================================================================
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

		List<UserProfileResponseDTO> searchedList = usrQryUseCase.searchUsers(searchWord.trim(), me.getUserId());

		log.info("{} 유저의 {} 검색결과 : {}", me.getNickname(), searchWord, searchedList);

		return ResponseEntity.ok(searchedList);
	}

	// ======[ 비밀번호 변경 ]===================================================================================================
	@PostMapping("/changePassword")
	public ResponseEntity<?> changePassword(@RequestBody Map<String, String> pwData) {
		try {
			String id = pwData.get("id");
			String prevPw = pwData.get("prevPw");
			String newPw = pwData.get("newPw");

			UserDTO dto = usrQryUseCase.getUser(id);

			log.info("비밀번호 일치? {} {}", prevPw, dto.getPassword());

			if (pwEncoder.matches(prevPw, dto.getPassword())) {

				boolean isChanged = usrCmdUseCase.changePassWord(id, newPw);
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
		List<UserDTO> list = usrQryUseCase.allUsers();

		return ResponseEntity.ok(list);
	}

}// chgPw
