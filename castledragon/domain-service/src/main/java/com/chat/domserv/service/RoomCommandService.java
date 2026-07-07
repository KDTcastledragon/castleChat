//package com.chat.domserv.service;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.chat.contract.room.domain.ChatRoomsDTO;
//import com.chat.contract.room.domain.ChatUserLookupDTO;
//import com.chat.contract.room.domain.RoomMembersDTO;
//import com.chat.contract.room.domain.res.EnterRoomResponseDTO;
//import com.chat.contract.room.domain.res.RoomMemberResponseDTO;
//import com.chat.contract.user.domain.SessionUserDTO;
//import com.chat.domserv.mapper.RoomMapper;
//import com.chat.domserv.mapper.UserMapper;
//import com.chat.domserv.usecase.RoomCommandUseCase;
//import com.chat.redis.cache.RoomMemberCache;
//
//import lombok.extern.log4j.Log4j2;
//
//@Service
//@Log4j2
//public class RoomCommandService implements RoomCommandUseCase {
//	@Autowired
//	RoomMapper roomMapper;
//
//	@Autowired
//	UserMapper userMapper;
//
//	@Autowired
//	RoomMemberCache roomMemberCache;
//
//	// ====== 방 생성 ==========================================================================================================================
//	private ChatRoomsDTO createRoom(String roomType, String roomStatus, String roomName, Long createdBy) {
//		ChatRoomsDTO room = new ChatRoomsDTO();
//		room.setRoomType(roomType);
//		room.setRoomStatus(roomStatus);
//		room.setRoomName(roomName);
//		room.setCreatedBy(createdBy);
//
//		int isInserted = roomMapper.createRoom(room); // dto 방식에서, parameter 방식으로 변경.하려 했으나,,, 결국 다시 왔소.
//		// chatMapper.createRoom("DIRECT", "ACTIVE");  // generated roomId를 받을 곳이 없음. 그래서 안 씀. ...
//		// 사실, 뭐 CreateRoomParam Helper Object를 만들수도 있기는 한데, 그래봐야 어차피 param.set~() --> param.getRoomId(); 해야되서 결국 조삼모사....
//		//		MyBatis INSERT는 원래 SELECT처럼 row를 반환하지 않아.
//		//		MariaDB/MySQL의 일반 INSERT는 PostgreSQL처럼 RETURNING *를 안정적으로 쓰는 구조가 아님.
//
//		if (isInserted != 1 || room.getRoomId() == null) {
//			log.error("채팅방 생성 실패 {}", room.getRoomId());
//			throw new IllegalStateException("채팅방 생성 실패");
//		}
//
//		return room;
//	}
//
//	// ====== 1:1 채팅방 만들기 ==========================================================================================================================
//	@Override
//	@Transactional
//	public EnterRoomResponseDTO getOrCreateDirectRoom(SessionUserDTO senderInfo, String friendPublicId) {
//		// roomStatus === 'DEACTIVATED'는 관리자만 컨트롤. dirRoom은 둘 다 left해도, deactive 하지 않음.
//
//		//  Long friendUserId = userMapper.findUserInfoByPublicId(friendPublicId).getUserId(); // 이건 DB 조회가 2번 나갈 가능성이 커. 같은 유저를 두 번 찾는 거라 낭비야.
//		//  String friendNickname = userMapper.findUserInfoByPublicId(friendPublicId).getNickname(); // 이건 DB 조회가 2번 나갈 가능성이 커. 같은 유저를 두 번 찾는 거라 낭비야.
//		ChatUserLookupDTO friendInfo = userMapper.findUserInfoByPublicId(friendPublicId);
//
//		if (friendInfo == null) {
//			throw new IllegalArgumentException("존재하지 않는 친구입니다.");
//		}
//
//		if (senderInfo.getUserId().equals(friendInfo.getUserId())) {
//			throw new IllegalArgumentException("s=f");
//		}
//
//		// 1. 기존 room 조회
//		ChatRoomsDTO room = roomMapper.findDirectRoom(senderInfo.getUserId(), friendInfo.getUserId());
//
//		if (room == null) {
//			log.info("새로운 채팅방(roomId) 생성 시작");
//
//			room = createRoom("DIRECT", "ACTIVE", "S:" + senderInfo.getUserId() + "T:" + friendInfo.getUserId(), senderInfo.getUserId());
//
//			log.info("새로운 채팅방의 newRoomId 생성 완료 : {}", room.getRoomId());
//
//			int isInsertedSender = roomMapper.insertRoomMember(room.getRoomId(), senderInfo.getUserId(), "MEMBER", friendInfo.getNickname()
//					+ "님과의 채팅방", friendInfo.getProfileImg(), "ACTIVE");
//			int isInsertedFriend = roomMapper.insertRoomMember(room.getRoomId(), friendInfo.getUserId(), "MEMBER", senderInfo.getNickname()
//					+ "님과의 채팅방", senderInfo.getProfileImg(), "ACTIVE");
//
//			if (isInsertedSender < 0 || isInsertedFriend < 0) {
//				throw new IllegalArgumentException("채팅방 생성 실패.");
//			}
//
//			roomMemberCache.initOrReplaceRoomMembers(room.getRoomId(), Set.of(senderInfo.getUserId(), friendInfo.getUserId()));
//
//			log.info("roomId={}  user1 : {}, user2 : {} newRoomCreated", room.getRoomId(), senderInfo.getUserId(), friendInfo.getUserId());
//		} else {
//			//			int isReactivatedSender = roomMapper.reactivateRoomMember(room.getRoomId(), senderInfo.getUserId());
//			//			int isReactivatedFriend = roomMapper.reactivateRoomMember(room.getRoomId(), friendInfo.getUserId());
//
//			List<Long> directMemberIds = List.of(senderInfo.getUserId(), friendInfo.getUserId());
//
//			int isReactivated = roomMapper.reactivateRoomMember(room.getRoomId(), directMemberIds);
//
//			if (isReactivated < 0) {
//				throw new IllegalArgumentException("재활성화 실패.");
//			}
//
//			//			Long finalRoomId = room.getRoomId();
//			//			roomMemberCache.getOrLoadRoomMembers(room.getRoomId(), () -> chatMapper.findActiveRoomMemberIds(room.getRoomId()));
//			//			roomMemberCache.getOrLoadRoomMembers(finalRoomId, () -> chatMapper.findActiveRoomMemberIds(finalRoomId));
//			// 여기서 finalRoomId를 쓰는 이유는 람다 안에서 사용하는 지역 변수는 Java에서 effectively final이어야 하기 때문이야. 
//			// roomId는 위에서 값이 바뀌었으니 람다 안에서 바로 쓰면 오류가 날 수 있어.
//
//			Set<Long> expectedMemberIds = Set.of(senderInfo.getUserId(), friendInfo.getUserId());
//			Set<Long> cachedMemberIds = roomMemberCache.getRoomMembers(room.getRoomId());
//
//			Set<Long> missingMemberIds = new HashSet<>(expectedMemberIds);
//			missingMemberIds.removeAll(cachedMemberIds);
//
//			if (!missingMemberIds.isEmpty()) {
//				roomMemberCache.addRoomMembers(room.getRoomId(), missingMemberIds);
//			}
//		}
//
//		// 3. DTO 조립
//		List<RoomMemberResponseDTO> friendProfile = new ArrayList<>();
//		friendProfile.add(new RoomMemberResponseDTO(friendInfo.getPublicId(), friendInfo.getNickname(), friendInfo.getFriendCode(), friendInfo
//				.getProfileImg(), "MEMBER"));
//
//		EnterRoomResponseDTO resRoom = new EnterRoomResponseDTO(room.getRoomId(), room.getRoomType(), friendInfo.getNickname()
//				+ "님과의 채팅방", friendInfo.getProfileImg(), 2L, friendProfile); // 굳이 2를 안 쓸 이유가 없다.
//
//		log.info("Direct resRoom : {}", resRoom);
//
//		return resRoom;
//	}
//
//	// ====== 단톡 채팅방 만들기 ==========================================================================================================================
//	@Override
//	@Transactional
//	public EnterRoomResponseDTO createGroupRoom(SessionUserDTO host, String customRoomName, String customRoomThumbnail, List<String> selectedFriendPublicIdList) {
//		// 1.friList 검증
//		if (selectedFriendPublicIdList == null || selectedFriendPublicIdList.isEmpty()) {
//			throw new IllegalArgumentException("초대할 친구가 없습니다.");
//		}
//
//		// 1. Front_end의 악의적/이상한 요청까지 막을 거냐”의 추가 정책 예외 처리.
//		if (selectedFriendPublicIdList.contains(host.getPublicId())) {
//			throw new IllegalArgumentException("자기 자신은 초대 대상에 포함할 수 없습니다.");
//		}
//
//		// 2. 초대 대상 bulk 조회
//		List<ChatUserLookupDTO> memberInfos = userMapper.findUserInfoByPublicIdList(selectedFriendPublicIdList);
//
//		// 3. 요청한 publicId 개수와 실제 조회된 유저 수 비교
//		if (memberInfos == null) {
//			throw new IllegalStateException("초대 대상 조회 실패");
//		} else if (memberInfos.size() != new HashSet<>(selectedFriendPublicIdList).size()) {
//			// 프론트가 보낸 publicId 목록에서 중복은 무시하고, 실제 존재하는 publicId가 전부 DB에서 조회됐는지 확인한다.
//			throw new IllegalArgumentException("존재하지 않는 초대 대상이 포함되어 있습니다.");
//		}
//
//		// 4. 검증 통과후 방 생성
//		if (customRoomName == null || customRoomName.trim().isEmpty()) {
//			customRoomName = host.getNickname() + "님의 단톡방";
//		}
//
//		ChatRoomsDTO createdRoom = createRoom("GROUP", "ACTIVE", "H:" + host.getUserId() + "M:"
//				+ (long) (selectedFriendPublicIdList.size() + 1), host.getUserId());
//
//		//		List<ChatUserLookupDTO> groupRoomMemberList = new ArrayList<>(); // list는 중복 제거가 좀 약하다. 그래서 map쓰자.
//		Map<Long, ChatUserLookupDTO> roomMemberMap = new LinkedHashMap<>(); // 굳이 HashMap 안쓰고 LinkedHash쓰는 이유는? 디버깅할때 좀 편하려고. 순서가 안정적이라 로그/응답 확인할 때 덜 헷갈림.
//
//		roomMemberMap.put(host.getUserId(), ChatUserLookupDTO.from(host));
//
//		//		for (String pubId : selectedFriendPublicIdList) {
//		//			ChatUserLookupDTO memberInfo = userMapper.findUserInfoByPublicId(pubId);
//		//			if (memberInfo != null) {
//		//				roomMemberMap.putIfAbsent(memberInfo.getUserId(), memberInfo); // 잘못된 publicId가 하나라도 섞이면 memberInfo.getUserId()에서 터진다.
//		//				// putIfAbsent 안 쓰고 put을 쓰면?  같은 유저가 중복으로 들어왔을 때 뒤 값으로 덮어써. 큰 문제는 아니지만, “처음 들어온 멤버 유지 + 중복 무시” 의도에 충실하게. 
//		//			}
//		//		}
//		// 기존 방식은 n명 -> n번 DB조회 라서 너무 조회가 많아진다. 이걸 바꾸는거다.bulk 조회로 바꾸는 게 맞아.
//
//		for (ChatUserLookupDTO memberInfo : memberInfos) {
//			if (memberInfo != null) {
//				roomMemberMap.putIfAbsent(memberInfo.getUserId(), memberInfo); // 잘못된 publicId가 하나라도 섞이면 memberInfo.getUserId()에서 터진다.
//				// putIfAbsent 안 쓰고 put을 쓰면?  같은 유저가 중복으로 들어왔을 때 뒤 값으로 덮어써. 큰 문제는 아니지만, “처음 들어온 멤버 유지 + 중복 무시” 의도에 충실하게. 
//			}
//		}
//
//		List<RoomMemberResponseDTO> roomMemberList = new ArrayList<>();
//
//		for (ChatUserLookupDTO member : roomMemberMap.values()) {
//			String role = member.getUserId().equals(host.getUserId()) ? "HOST" : "MEMBER";
//
//			roomMapper.insertRoomMember(createdRoom.getRoomId(), member.getUserId(), role, customRoomName, customRoomThumbnail, "ACTIVE");
//
//			roomMemberList.add(new RoomMemberResponseDTO(member.getPublicId(), member.getNickname(), member.getFriendCode(), member
//					.getProfileImg(), role));
//		}
//
//		roomMemberCache.initOrReplaceRoomMembers(createdRoom.getRoomId(), new LinkedHashSet<>(roomMemberMap.keySet()));
//
//		EnterRoomResponseDTO resRoom = new EnterRoomResponseDTO(createdRoom.getRoomId(), createdRoom
//				.getRoomType(), customRoomName, customRoomThumbnail, (long) roomMemberList.size(), roomMemberList);
//
//		return resRoom;
//
//	}// createGroupRoom
//
//	// ====== 단체 채팅방에 유저 초대 ============================================================================================================================
//	@Override
//	@Transactional
//	public int inviteGroupRoom(Long roomId, SessionUserDTO inviter, List<String> inviteTargetMemberPublicIds) {
//		if (roomId == null) {
//			throw new IllegalArgumentException("roomId가 없습니다.");
//		}
//
//		if (inviteTargetMemberPublicIds == null || inviteTargetMemberPublicIds.isEmpty()) {
//			throw new IllegalArgumentException("초대할 친구가 없습니다.");
//		}
//
//		ChatRoomsDTO existedRoom = roomMapper.getRoomByRoomId(roomId);
//
//		if (existedRoom == null) {
//			throw new IllegalArgumentException("존재하지 않는 방입니다.");
//		}
//
//		if (!existedRoom.getRoomStatus().equals("ACTIVE")) {
//			throw new IllegalArgumentException("비활성화 된 방입니다.");
//		}
//
//		RoomMembersDTO roomMeberAuth = roomMapper.getActiveRoomMemberInfoInRoom(roomId, inviter.getUserId());
//		if (roomMeberAuth == null) {
//			throw new IllegalArgumentException("현재 채팅방의 멤버가 아닙니다.");
//		}
//
//		String inviterRole = roomMapper.findRoleInRoomByUserId(roomId, inviter.getUserId());
//		if (!"HOST".equals(inviterRole) && !"MANAGER".equals(inviterRole)) {
//			throw new IllegalArgumentException("초대권한이 없습니다.");
//		}
//
//		if (inviteTargetMemberPublicIds == null || inviteTargetMemberPublicIds.isEmpty()) {
//			throw new IllegalArgumentException("초대할 친구가 없습니다.");
//		}
//
//		if (inviteTargetMemberPublicIds.contains(inviter.getPublicId())) {
//			throw new IllegalArgumentException("자기 자신은 초대 대상에 포함할 수 없습니다.");
//		}
//
//		// 2. 초대 대상 bulk 조회
//		List<ChatUserLookupDTO> inviteMemberInfos = userMapper.findUserInfoByPublicIdList(inviteTargetMemberPublicIds);
//
//		// 3. 요청한 publicId 개수와 실제 조회된 유저 수 비교
//		if (inviteMemberInfos == null) {
//			throw new IllegalStateException("초대 대상 조회 실패");
//		} else if (inviteMemberInfos.size() != new HashSet<>(inviteTargetMemberPublicIds).size()) {
//			// 프론트가 보낸 publicId 목록에서 중복은 무시하고, 실제 존재하는 publicId가 전부 DB에서 조회됐는지 확인한다.
//			throw new IllegalArgumentException("존재하지 않는 초대 대상이 포함되어 있습니다.");
//		}
//
//		int invitedCount = 0;
//		Set<Long> invitedUserIds = new HashSet<>();
//		for (ChatUserLookupDTO mbr : inviteMemberInfos) {
//			int inserted = roomMapper
//					.insertRoomMember(roomId, mbr.getUserId(), "MEMBER", existedRoom.getRoomName(), existedRoom.getRoomThumbnail(), "ACTIVE");
//
//			if (inserted != 1) {
//				throw new IllegalStateException("초대 처리 실패: userId=" + mbr.getUserId());
//			}
//
//			invitedUserIds.add(mbr.getUserId());
//
//			invitedCount += inserted;
//		}
//
//		roomMemberCache.addRoomMembers(roomId, invitedUserIds);
//
//		return invitedCount;
//	}
//
//	// ====== 방 나가기 (자발적) =================================================================================================================
//	@Override
//	@Transactional
//	public boolean leftRoom(Long roomId, SessionUserDTO me) {
//		if (roomId == null) {
//			throw new IllegalArgumentException("roomId가 없습니다.");
//		}
//
//		RoomMembersDTO myMemberInfoInRoom = roomMapper.getActiveRoomMemberInfoInRoom(roomId, me.getUserId());
//
//		if (myMemberInfoInRoom == null) {
//			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
//		}
//
//		if ("HOST".equals(myMemberInfoInRoom.getRole())) {
//			throw new IllegalStateException("방장은 아직 나갈 수 없습니다.");
//		}
//
//		//		if (!"ACTIVE".equals(myMemberInfoInRoom.getMemberStatus())) {
//		//			throw new IllegalArgumentException("방에 참여중인 멤버만 나갈 수 있습니다.");
//		//		}
//
//		int isLefted = roomMapper.leftRoom(roomId, me.getUserId());
//
//		if (isLefted < 1) {
//			throw new IllegalArgumentException("방 나가기 실패.");
//		}
//
//		roomMemberCache.removeRoomMember(roomId, me.getUserId());
//
//		return true;
//	}
//
//	// ====== 멤버 강퇴(영구 강퇴 x) ========================================================================================================
//	@Override
//	@Transactional
//	public int kickMemberInRoom(Long roomId, SessionUserDTO kicker, String kickTargetPublicId) {
//		if (roomId == null || kickTargetPublicId == null) {
//			log.info("필수 파라미터 누락. : {} {}", roomId, kickTargetPublicId);
//		}
//
//		ChatRoomsDTO existedRoom = roomMapper.getRoomByRoomId(roomId);
//
//		if (existedRoom == null) {
//			throw new IllegalArgumentException("존재하지 않는 방입니다.");
//		}
//
//		if (!existedRoom.getRoomStatus().equals("ACTIVE")) {
//			throw new IllegalArgumentException("비활성화 된 방입니다.");
//		}
//
//		RoomMembersDTO kickerInfo = roomMapper.getActiveRoomMemberInfoInRoom(roomId, kicker.getUserId());
//		if (kickerInfo == null) {
//			throw new IllegalArgumentException("존재 하지않는 강퇴자 입니다.");
//		}
//
//		if (kickTargetPublicId.equals(kicker.getPublicId())) {
//			throw new IllegalArgumentException("자기 자신은 강퇴할 수 없습니다.");
//		}
//
//		if (!"HOST".equals(kickerInfo.getRole()) && !"MANAGER".equals(kickerInfo.getRole())) {
//			throw new IllegalArgumentException("강퇴 권한이 없습니다.");
//		}
//
//		Long kickTargetMemberUserId = userMapper.findUserIdByPublicId(kickTargetPublicId);
//
//		if (kickTargetMemberUserId == null) {
//			throw new IllegalArgumentException("존재하지 않는 강퇴 대상입니다.");
//		}
//
//		RoomMembersDTO kickTargetMemberInfo = roomMapper.getActiveRoomMemberInfoInRoom(roomId, kickTargetMemberUserId);
//
//		if (kickTargetMemberInfo == null) {
//			throw new IllegalArgumentException("강퇴 대상이 현재 채팅방의 멤버가 아닙니다.");
//		}
//
//		if (kickTargetMemberInfo.getRole().equals("HOST")) {
//			throw new IllegalArgumentException("방장은 강퇴할 수 없습니다.");
//		}
//
//		if ((kickerInfo.getRole().equals("MANAGER")) && (kickTargetMemberInfo.getRole().equals("MANAGER"))) {
//			throw new IllegalArgumentException("매니저는 일반 유저만 강퇴할 수 있습니다.");
//		}
//
//		int isKicked = roomMapper.kickMemberInRoom(roomId, kicker.getUserId(), kickTargetMemberUserId);
//
//		if (isKicked < 1) {
//			throw new IllegalArgumentException("강퇴 실패.");
//		}
//
//		roomMemberCache.removeRoomMember(roomId, kickTargetMemberUserId);
//
//		return isKicked;
//
//	}
//
//	@Override
//	public int banMemberInRoom(Long roomId, SessionUserDTO banner, String banMemberPublicIds) {
//
//		return 0;
//	}
//
//	@Override
//	public int changeMemberRoleInRoom(Long roomId, SessionUserDTO me, String targetPublicId) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//}
