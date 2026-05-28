package com.chat.castledragon.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, Object> data, HttpSession session) {
		log.info("loginData :" + data);
		String id = (String) data.get("id");
		String pw = (String) data.get("pw");

		UserDTO user = userService.login(id, pw);

		if (user == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("로그인 실패");
		}

		session.setAttribute("LOGIN_USER", user);

		return ResponseEntity.ok(user);
	}// login

	@PostMapping("/login2")
	public ResponseEntity<?> login2(@RequestBody Map<String, Object> data) {
		log.info("loginData :" + data);
		String id = (String) data.get("id");
		String pw = (String) data.get("pw");

		UserDTO user = userService.login(id, pw);

		if (user != null) {
			return ResponseEntity.ok(user);
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("잘못된 아이디");
		}
	}

	@GetMapping("/allUsers")
	public ResponseEntity<?> allUsers() {
		//		log.info("allUsers");
		List<UserDTO> list = userService.allUsers();

		return ResponseEntity.ok(list);
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session) {
		session.invalidate();
		return ResponseEntity.ok().build();
	}

}

//@PostMapping("/friendList")
//public ResponseEntity<?> friendList(@RequestBody Map<String, Object> data) {
//	try {
//		log.info("data for friendList: " + data);
//		String user_id = (String) data.get("user_id");
//		List<UserDTO> fri_list = userservice.friendList(user_id);
//		log.info("id&fri_list : " + user_id + fri_list);
//
//		return ResponseEntity.ok(fri_list);
//
//	} catch (Exception e) {
//		throw e;
//	}
//}
