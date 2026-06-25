//package com.chat.domserv.service;
//
//import org.springframework.stereotype.Service;
//
//import lombok.extern.log4j.Log4j2;
//
//@Service
//@Log4j2
//public class ChatServiceImpl_legacy {
//
//}//serviceImpl
//
////	@Override
////	@Transactional
////	public EnterRoomResponseDTO createGroupRoom(SessionUserDTO host, String roomName, List<String> selectedFriendPublicIdList) {
////
////		if (roomName == null || roomName.trim().isEmpty()) {
////			roomName = host.getNickname() + "님의 단톡방";
////		}
////
////		ChatRoomDTO createdRoom = createRoom("GROUP", "ACTIVE", "H:" + host.getUserId() + "M:" + (selectedFriendPublicIdList.size() + 1), host.getUserId());
////
////		List<ChatMemberDTO> groupRoomMemberList = new ArrayList<>(); // UserProfileResponseDTO 대신, 내부 Mappiing용으로 GroupRoomMemberDTO 타입 생성 .
////
////		// 1. pubId로 userId찾아서 insert를 위한 profileInfo getting.
////
////		// 방장 먼저 넣어줌.
////		ChatMemberDTO hostInfo = ChatMemberDTO.from(host); // 정적 팩토리 메소드(static factory method). 자세한 설명은 chatMdto ㄱㄱ.
////		groupRoomMemberList.add(hostInfo);
////
////		for (String pubId : selectedFriendPublicIdList) {
////			ChatMemberDTO memberInfo = userMapper.findUserInfoByPublicId(pubId);
////			// insertList.add(member); // 여기서 추가 안하고 밑의 if문에서 추가.
////
////			if (memberInfo != null) {
////				groupRoomMemberList.add(memberInfo);
////			}
////
////		} // groupRoomMemberList에 ChatMemberDTO 형태로 모든 단톡방 멤버 정보 add.
////
////		// 2. gRML에서 userId만 추출하여 insert 사전 작업 하기.
////		Set<Long> groupRoomMemberUserIdSet = new LinkedHashSet<>();
////		//		groupRoomMemberUserIdSet.add(hostInfo.getUserId()); // 이미 groupRoomMemberList에 .add(hostInfo)했기 때문에, 중벅이라 안 써도딘당.
////
////		for (ChatMemberDTO member : groupRoomMemberList) {
////			groupRoomMemberUserIdSet.add(member.getUserId());
////		}
////
////		// 3. insert
////		for (Long id : groupRoomMemberUserIdSet) {
////			String role = id.equals(host.getUserId()) ? "HOST" : "MEMBER";
////			chatMapper.insertRoomMember(createdRoom.getRoomId(), id, role, roomName);
////		}
////
////		// 단톡방 session
////		roomMemberCache.cacheRoomMembers(createdRoom.getRoomId(), groupRoomMemberUserIdSet);
////
////		// 4. mL profile재조립 및 response Data 조립
////		List<RoomMemberResponseDTO> responseMemberList = new ArrayList<>();
////
////		for (ChatMemberDTO m : groupRoomMemberList) {
////			responseMemberList.add(new RoomMemberResponseDTO(m.getPublicId(), m.getNickname(), m.getFriendCode(), m.getProfileImg(), 역할));
////		}
////
////		Long roomMemberCount = (long) responseMemberList.size();
////
////		EnterRoomResponseDTO resData = new EnterRoomResponseDTO(createdRoom.getRoomId(), createdRoom.getRoomType(), roomName, roomMemberCount, responseMemberList, null);
////
////		log.info("GroupCreated : {} {} {} {} {}", createdRoom.getRoomId(), createdRoom.getRoomType(), roomName, roomMemberCount, responseMemberList);
////
////		return resData;
////
////	}// createGroupRoom
//
////
////
////	@Override
////	public Long insertMessage(Long roomId, Long senderId, String msgText) {
////		ChatDTO dto = new ChatDTO();
////		dto.setRoomId(roomId);
////		dto.setSenderId(senderId);
////		dto.setMsgText(msgText);
////
////		chatMapper.insertMessage(dto); //과거의 params.put 방식도 사용 가능하나, 오타위험 안정성 낮음 등의 이유로 dto가 더 낫다. 실무도 dto 더 많이씀.
////		Long messageId = dto.getMessageId();
////
////		return messageId;
////	}
//
////if (roomId != null) {
////	log.info("{}과 {}의 채팅방 이미 존재 : {}", user1, user2, roomId);
////	return roomId;
////}
//// 
////// 이걸 Guard Clause(가드 절) 또는 Early Return 패턴이라고 한다. if로 방어 조건을 먼저 빼는 것.
////
////// 2. room 생성.  if 다음 else 안 쓰는 이유? 1. nesting(중첩)때문에.가독성저하.조건흐름추적어려움.  2.Pyramid of Doom형태로 else의 else의 else...가 되버리기 때문.
////ChatRoomsDTO room = new ChatRoomsDTO();
////
////room.setRoomType("DIRECT");
////room.setRoomStatus("ACTIVE");
////
////chatMapper.createRoom(room);
////log.info("채팅방 새로 생성 :" + room);
////
////Long newRoomId = room.getRoomId();
////log.info("새로운 채팅방의 newRoomId :" + newRoomId);
////
////// 3. room member insert
////chatMapper.insertRoomMember(newRoomId, user1);
////chatMapper.insertRoomMember(newRoomId, user2);
////log.info("roomId={}  user1 : {}, user2 : {} 추가", newRoomId, user1, user2);
////
////return newRoomId;
