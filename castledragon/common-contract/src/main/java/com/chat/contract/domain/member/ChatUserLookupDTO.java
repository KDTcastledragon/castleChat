package com.chat.contract.domain.member;

import com.chat.contract.domain.user.SessionUserDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 내부 join용 DTO.  publicId로 userId 조회 & publicId로 profile 조회 & userProfile조립 --> query를 2배로 해야됨. 그냥 column중복되도 dto새로 하나 파는게 낫다.
public class ChatUserLookupDTO {
	private Long userId;

	private String publicId;

	private String nickname;

	private String friendCode;

	private String profileImg;

	public static ChatUserLookupDTO from(SessionUserDTO me) {
		return new ChatUserLookupDTO(me.getUserId(), me.getPublicId(), me.getNickname(), me.getFriendCode(), me.getProfileImg());
	}

	// 가능은하다. 공부용으로 임시 추가.
	// 필요하면 from을 여러 개 만들 수 있어. 이걸 오버로딩이라고 해.
	//	public static ChatUserLookupDTO from(UserDTO user) {
	//		return new ChatUserLookupDTO(user.getUserId(), user.getPublicId(), user.getNickname(), user.getFriendCode(), user.getProfileImg());
	//	}

	// static : 객체를 먼저 만들지 않고 클래스 이름으로 바로 호출 가능. DTO me = new DTO() 이런거 일일이 안해도된다. --> ChatMemberDTO야, 이 me를 바탕으로 네 객체 하나 만들어줘
	// .from : “객체를 만드는 메소드”.  LocalDate.of(2026, 6, 5); String.valueOf(123); --> 이런 것도 같은 계열이야.
	// 생성자를 대체하는 게 아니라, 생성자를 내부에서 감싸서 더 읽기 좋게 쓰는 메소드야.
}
