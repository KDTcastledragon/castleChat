package com.chat.castledragon.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.castledragon.cache.RoomMemberCache;
import com.chat.castledragon.domain.ChatMessageDTO;
import com.chat.castledragon.domain.ChatMessageResponseDTO;
import com.chat.castledragon.domain.ChatRoomDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.ChatUserLookupDTO;
import com.chat.castledragon.domain.EnterRoomResponseDTO;
import com.chat.castledragon.domain.PayloadSendMessageDTO;
import com.chat.castledragon.domain.RoomMemberResponseDTO;
import com.chat.castledragon.domain.SessionUserDTO;
import com.chat.castledragon.mapper.ChatMapper;
import com.chat.castledragon.mapper.UserMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ChatServiceImpl implements ChatService {

	@Autowired
	ChatMapper chatMapper;

	@Autowired
	UserMapper userMapper;

	@Autowired
	RoomMemberCache roomMemberCache;

	// ====== 방 생성 ==========================================================================================================================
	private ChatRoomDTO createRoom(String roomType, String roomStatus, String roomName, Long createdBy) {
		ChatRoomDTO room = new ChatRoomDTO();
		room.setRoomType(roomType);
		room.setRoomStatus(roomStatus);
		room.setRoomName(roomName);
		room.setCreatedBy(createdBy);

		int isInserted = chatMapper.createRoom(room); // dto 방식에서, parameter 방식으로 변경.하려 했으나,,, 결국 다시 왔소.
		// chatMapper.createRoom("DIRECT", "ACTIVE");  // generated roomId를 받을 곳이 없음. 그래서 안 씀. ...
		// 사실, 뭐 CreateRoomParam Helper Object를 만들수도 있기는 한데, 그래봐야 어차피 param.set~() --> param.getRoomId(); 해야되서 결국 조삼모사....
		//		MyBatis INSERT는 원래 SELECT처럼 row를 반환하지 않아.
		//		MariaDB/MySQL의 일반 INSERT는 PostgreSQL처럼 RETURNING *를 안정적으로 쓰는 구조가 아님.

		if (isInserted != 1 || room.getRoomId() == null) {
			log.error("채팅방 생성 실패 {}", room.getRoomId());
			throw new IllegalStateException("채팅방 생성 실패");
		}

		return room;
	}

	// ====== 1:1 채팅방 만들기 ==========================================================================================================================
	@Override
	@Transactional
	public EnterRoomResponseDTO getOrCreateDirectRoom(SessionUserDTO senderInfo, String friendPublicId) {

		//  Long friendUserId = userMapper.findUserInfoByPublicId(friendPublicId).getUserId(); // 이건 DB 조회가 2번 나갈 가능성이 커. 같은 유저를 두 번 찾는 거라 낭비야.
		//  String friendNickname = userMapper.findUserInfoByPublicId(friendPublicId).getNickname(); // 이건 DB 조회가 2번 나갈 가능성이 커. 같은 유저를 두 번 찾는 거라 낭비야.
		ChatUserLookupDTO friendInfo = userMapper.findUserInfoByPublicId(friendPublicId);

		if (friendInfo == null) {
			throw new IllegalArgumentException("존재하지 않는 친구입니다.");
		}

		// 1. 기존 room 조회
		ChatRoomDTO room = chatMapper.findDirectRoom(senderInfo.getUserId(), friendInfo.getUserId());

		if (room == null) {
			log.info("새로운 채팅방(roomId) 생성 시작");

			//			ChatRoomDTO newCreatedRoom = createRoom("DIRECT", "ACTIVE", "S:" + senderInfo.getUserId() + "T:" + friendInfo.getUserId(), senderInfo.getUserId());
			room = createRoom("DIRECT", "ACTIVE", "S:" + senderInfo.getUserId() + "T:" + friendInfo.getUserId(), senderInfo.getUserId());

			log.info("새로운 채팅방의 newRoomId 생성 완료 : {}", room.getRoomId());

			chatMapper.insertRoomMember(room.getRoomId(), senderInfo.getUserId(), "MEMBER", friendInfo.getNickname()
					+ "님과의 채팅방", friendInfo.getProfileImg(), "ACTIVE");
			chatMapper.insertRoomMember(room.getRoomId(), friendInfo.getUserId(), "MEMBER", senderInfo.getNickname()
					+ "님과의 채팅방", senderInfo.getProfileImg(), "ACTIVE");

			roomMemberCache.initOrReplaceRoomMembers(room.getRoomId(), Set.of(senderInfo.getUserId(), friendInfo.getUserId()));

			log.info("roomId={}  user1 : {}, user2 : {} newRoomCreated", room.getRoomId(), senderInfo.getUserId(), friendInfo.getUserId());
		} else {
			Long finalRoomId = room.getRoomId();
			roomMemberCache.getOrLoadRoomMembers(finalRoomId, () -> chatMapper.findActiveRoomMemberIds(finalRoomId));
			// 여기서 finalRoomId를 쓰는 이유는 람다 안에서 사용하는 지역 변수는 Java에서 effectively final이어야 하기 때문이야. 
			// roomId는 위에서 값이 바뀌었으니 람다 안에서 바로 쓰면 오류가 날 수 있어.
		}

		// 3. DTO 조립
		List<RoomMemberResponseDTO> friendProfile = new ArrayList<>();
		friendProfile.add(new RoomMemberResponseDTO(friendInfo.getPublicId(), friendInfo.getNickname(), friendInfo.getFriendCode(), friendInfo.getProfileImg(), "MEMBER"));

		EnterRoomResponseDTO resRoom = new EnterRoomResponseDTO(room.getRoomId(), room.getRoomType(), friendInfo.getNickname()
				+ "님과의 채팅방", friendInfo.getProfileImg(), 2L, friendProfile, null); // 굳이 2를 안 쓸 이유가 없다.

		log.info("Direct resRoom : {}", resRoom);

		return resRoom;
	}

	// ====== 단톡 채팅방 만들기 ==========================================================================================================================
	@Override
	@Transactional
	public EnterRoomResponseDTO createGroupRoom(SessionUserDTO host, String customRoomName, String customRoomThumbnail, List<String> selectedFriendPublicIdList) {
		// 1.friList 검증
		if (selectedFriendPublicIdList == null || selectedFriendPublicIdList.isEmpty()) {
			throw new IllegalArgumentException("초대할 친구가 없습니다.");
		}

		// 1. Front_end의 악의적/이상한 요청까지 막을 거냐”의 추가 정책 예외 처리.
		if (selectedFriendPublicIdList.contains(host.getPublicId())) {
			throw new IllegalArgumentException("자기 자신은 초대 대상에 포함할 수 없습니다.");
		}

		// 2. 초대 대상 bulk 조회
		List<ChatUserLookupDTO> memberInfos = userMapper.findUserInfoByPublicIdList(selectedFriendPublicIdList);

		// 3. 요청한 publicId 개수와 실제 조회된 유저 수 비교
		if (memberInfos == null) {
			throw new IllegalStateException("초대 대상 조회 실패");
		} else if (memberInfos.size() != new HashSet<>(selectedFriendPublicIdList).size()) {
			// 프론트가 보낸 publicId 목록에서 중복은 무시하고, 실제 존재하는 publicId가 전부 DB에서 조회됐는지 확인한다.
			throw new IllegalArgumentException("존재하지 않는 초대 대상이 포함되어 있습니다.");
		}

		// 4. 검증 통과후 방 생성
		if (customRoomName == null || customRoomName.trim().isEmpty()) {
			customRoomName = host.getNickname() + "님의 단톡방";
		}

		ChatRoomDTO createdRoom = createRoom("GROUP", "ACTIVE", "H:" + host.getUserId() + "M:" + (long) (selectedFriendPublicIdList.size() + 1), host.getUserId());

		//		List<ChatUserLookupDTO> groupRoomMemberList = new ArrayList<>(); // list는 중복 제거가 좀 약하다. 그래서 map쓰자.
		Map<Long, ChatUserLookupDTO> roomMemberMap = new LinkedHashMap<>(); // 굳이 HashMap 안쓰고 LinkedHash쓰는 이유는? 디버깅할때 좀 편하려고. 순서가 안정적이라 로그/응답 확인할 때 덜 헷갈림.

		roomMemberMap.put(host.getUserId(), ChatUserLookupDTO.from(host));

		//		for (String pubId : selectedFriendPublicIdList) {
		//			ChatUserLookupDTO memberInfo = userMapper.findUserInfoByPublicId(pubId);
		//			if (memberInfo != null) {
		//				roomMemberMap.putIfAbsent(memberInfo.getUserId(), memberInfo); // 잘못된 publicId가 하나라도 섞이면 memberInfo.getUserId()에서 터진다.
		//				// putIfAbsent 안 쓰고 put을 쓰면?  같은 유저가 중복으로 들어왔을 때 뒤 값으로 덮어써. 큰 문제는 아니지만, “처음 들어온 멤버 유지 + 중복 무시” 의도에 충실하게. 
		//			}
		//		}
		// 기존 방식은 n명 -> n번 DB조회 라서 너무 조회가 많아진다. 이걸 바꾸는거다.bulk 조회로 바꾸는 게 맞아.

		for (ChatUserLookupDTO memberInfo : memberInfos) {
			if (memberInfo != null) {
				roomMemberMap.putIfAbsent(memberInfo.getUserId(), memberInfo); // 잘못된 publicId가 하나라도 섞이면 memberInfo.getUserId()에서 터진다.
				// putIfAbsent 안 쓰고 put을 쓰면?  같은 유저가 중복으로 들어왔을 때 뒤 값으로 덮어써. 큰 문제는 아니지만, “처음 들어온 멤버 유지 + 중복 무시” 의도에 충실하게. 
			}
		}

		List<RoomMemberResponseDTO> roomMemberList = new ArrayList<>();

		for (ChatUserLookupDTO member : roomMemberMap.values()) {
			String role = member.getUserId().equals(host.getUserId()) ? "HOST" : "MEMBER";

			chatMapper.insertRoomMember(createdRoom.getRoomId(), member.getUserId(), role, customRoomName, customRoomThumbnail, "ACTIVE");

			roomMemberList.add(new RoomMemberResponseDTO(member.getPublicId(), member.getNickname(), member.getFriendCode(), member.getProfileImg(), role));
		}

		roomMemberCache.initOrReplaceRoomMembers(createdRoom.getRoomId(), new LinkedHashSet<>(roomMemberMap.keySet()));

		EnterRoomResponseDTO resRoom = new EnterRoomResponseDTO(createdRoom.getRoomId(), createdRoom.getRoomType(), customRoomName, customRoomThumbnail, (long) roomMemberList.size(), roomMemberList, null);

		return resRoom;

	}// createGroupRoom

	// ====== 메시지 보내기 ==========================================================================================================================
	@Override
	@Transactional
	public ChatMessageResponseDTO sendMessage(Long senderUserId, String senderPublicId, PayloadSendMessageDTO payload, Set<Long> viewingUserIds) {
		Long roomId = payload.getRoomId();

		// WsHandler에서 검사하긴 했지만, Service에서도 독립적인 방어 필요함.
		if (roomId == null) {
			throw new IllegalArgumentException("roomId가 없습니다.");
		}

		if (payload.getMessageText() == null) {
			throw new IllegalArgumentException("메시지 내용이 없습니다.");
		}

		// 방 멤버 전체를 Redis에서 가져온다. 없으면 DB에서 가져와 Redis에 올린다.
		Set<Long> totalRoomMemberIds = roomMemberCache.getOrLoadRoomMembers(roomId, () -> chatMapper.findActiveRoomMemberIds(roomId));

		if (totalRoomMemberIds.isEmpty()) {
			throw new IllegalStateException("채팅방 멤버 정보를 찾을 수 없습니다.");
		}

		if (!totalRoomMemberIds.contains(senderUserId)) {
			throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
		}

		LocalDateTime now = LocalDateTime.now(); // 서버시간 기준으로 한다. FE에서 time을 조작할 수도 있기 때문이다. 우린 서버를 신뢰한다.
		// DB에서 created_at default current_timestamp로 넣는 구조면, insert 후에 MyBatis가 createdAt까지 자동으로 채워주지는 않아. useGeneratedKeys로 보통 messageId만 들어와.

		ChatMessageDTO insertChat = new ChatMessageDTO();
		insertChat.setRoomId(roomId);
		insertChat.setSenderId(senderUserId);
		insertChat.setMessageText(payload.getMessageText());
		insertChat.setCreatedAt(now);
		chatMapper.insertMessage(insertChat); // DB에 Msg 저장.

		// 현재 방을 보고 있는 사람들은 메시지를 즉시 받은 상태니까 읽은 사람으로 본다. 보낸 사람도 자기 메시지는 당연히 읽은 상태니까 추가한다.
		Set<Long> viewingRoomMemberIds = new HashSet<>(viewingUserIds);
		viewingRoomMemberIds.add(senderUserId);

		viewingRoomMemberIds.retainAll(totalRoomMemberIds); // 혹시 이상한 userId가 섞였더라도 실제 방 멤버만 남긴다.

		log.info("{}방의 총 유저 : {}  , 현재 연결된 유저 : {}", roomId, totalRoomMemberIds, viewingRoomMemberIds);

		Long unreadCount = (long) (totalRoomMemberIds.size() - viewingRoomMemberIds.size()); // 안 읽은 사람 수 계산.

		log.info("unreadCount : {}", unreadCount);

		if (unreadCount < 0) {
			unreadCount = 0L; // == (long) 0;
		}

		ChatMessageResponseDTO resChat = new ChatMessageResponseDTO();
		resChat.setMessageId(insertChat.getMessageId());
		resChat.setRoomId(insertChat.getRoomId());
		resChat.setSenderPublicId(senderPublicId);
		resChat.setMessageText(insertChat.getMessageText());
		resChat.setCreatedAt(insertChat.getCreatedAt());
		resChat.setUnreadCount(unreadCount);

		log.info("chatServ -> wsHandler 채팅data 이동 : {}", resChat);

		return resChat;
	}

	@Override
	public List<ChatMessageResponseDTO> getPrevMessagesInRoom(Long roomId) {
		List<ChatMessageResponseDTO> chatList = chatMapper.getPrevMessagesInRoom(roomId);
		return chatList;
	}

	@Override
	public void updateLastRead(Long roomId, Long userId, Long lastReadMessageId) {
		chatMapper.updateLastRead(roomId, userId, lastReadMessageId);
	}

	@Override
	public List<ChatRoomListDTO> getMyAllRooms(Long userId) {
		List<ChatRoomListDTO> roomList = chatMapper.getMyChatRooms(userId);
		return roomList;
	}

}//serviceImpl

//	@Override
//	@Transactional
//	public EnterRoomResponseDTO createGroupRoom(SessionUserDTO host, String roomName, List<String> selectedFriendPublicIdList) {
//
//		if (roomName == null || roomName.trim().isEmpty()) {
//			roomName = host.getNickname() + "님의 단톡방";
//		}
//
//		ChatRoomDTO createdRoom = createRoom("GROUP", "ACTIVE", "H:" + host.getUserId() + "M:" + (selectedFriendPublicIdList.size() + 1), host.getUserId());
//
//		List<ChatMemberDTO> groupRoomMemberList = new ArrayList<>(); // UserProfileResponseDTO 대신, 내부 Mappiing용으로 GroupRoomMemberDTO 타입 생성 .
//
//		// 1. pubId로 userId찾아서 insert를 위한 profileInfo getting.
//
//		// 방장 먼저 넣어줌.
//		ChatMemberDTO hostInfo = ChatMemberDTO.from(host); // 정적 팩토리 메소드(static factory method). 자세한 설명은 chatMdto ㄱㄱ.
//		groupRoomMemberList.add(hostInfo);
//
//		for (String pubId : selectedFriendPublicIdList) {
//			ChatMemberDTO memberInfo = userMapper.findUserInfoByPublicId(pubId);
//			// insertList.add(member); // 여기서 추가 안하고 밑의 if문에서 추가.
//
//			if (memberInfo != null) {
//				groupRoomMemberList.add(memberInfo);
//			}
//
//		} // groupRoomMemberList에 ChatMemberDTO 형태로 모든 단톡방 멤버 정보 add.
//
//		// 2. gRML에서 userId만 추출하여 insert 사전 작업 하기.
//		Set<Long> groupRoomMemberUserIdSet = new LinkedHashSet<>();
//		//		groupRoomMemberUserIdSet.add(hostInfo.getUserId()); // 이미 groupRoomMemberList에 .add(hostInfo)했기 때문에, 중벅이라 안 써도딘당.
//
//		for (ChatMemberDTO member : groupRoomMemberList) {
//			groupRoomMemberUserIdSet.add(member.getUserId());
//		}
//
//		// 3. insert
//		for (Long id : groupRoomMemberUserIdSet) {
//			String role = id.equals(host.getUserId()) ? "HOST" : "MEMBER";
//			chatMapper.insertRoomMember(createdRoom.getRoomId(), id, role, roomName);
//		}
//
//		// 단톡방 session
//		roomMemberCache.cacheRoomMembers(createdRoom.getRoomId(), groupRoomMemberUserIdSet);
//
//		// 4. mL profile재조립 및 response Data 조립
//		List<RoomMemberResponseDTO> responseMemberList = new ArrayList<>();
//
//		for (ChatMemberDTO m : groupRoomMemberList) {
//			responseMemberList.add(new RoomMemberResponseDTO(m.getPublicId(), m.getNickname(), m.getFriendCode(), m.getProfileImg(), 역할));
//		}
//
//		Long roomMemberCount = (long) responseMemberList.size();
//
//		EnterRoomResponseDTO resData = new EnterRoomResponseDTO(createdRoom.getRoomId(), createdRoom.getRoomType(), roomName, roomMemberCount, responseMemberList, null);
//
//		log.info("GroupCreated : {} {} {} {} {}", createdRoom.getRoomId(), createdRoom.getRoomType(), roomName, roomMemberCount, responseMemberList);
//
//		return resData;
//
//	}// createGroupRoom

//
//
//	@Override
//	public Long insertMessage(Long roomId, Long senderId, String msgText) {
//		ChatDTO dto = new ChatDTO();
//		dto.setRoomId(roomId);
//		dto.setSenderId(senderId);
//		dto.setMsgText(msgText);
//
//		chatMapper.insertMessage(dto); //과거의 params.put 방식도 사용 가능하나, 오타위험 안정성 낮음 등의 이유로 dto가 더 낫다. 실무도 dto 더 많이씀.
//		Long messageId = dto.getMessageId();
//
//		return messageId;
//	}

//if (roomId != null) {
//	log.info("{}과 {}의 채팅방 이미 존재 : {}", user1, user2, roomId);
//	return roomId;
//}
// 
//// 이걸 Guard Clause(가드 절) 또는 Early Return 패턴이라고 한다. if로 방어 조건을 먼저 빼는 것.
//
//// 2. room 생성.  if 다음 else 안 쓰는 이유? 1. nesting(중첩)때문에.가독성저하.조건흐름추적어려움.  2.Pyramid of Doom형태로 else의 else의 else...가 되버리기 때문.
//ChatRoomsDTO room = new ChatRoomsDTO();
//
//room.setRoomType("DIRECT");
//room.setRoomStatus("ACTIVE");
//
//chatMapper.createRoom(room);
//log.info("채팅방 새로 생성 :" + room);
//
//Long newRoomId = room.getRoomId();
//log.info("새로운 채팅방의 newRoomId :" + newRoomId);
//
//// 3. room member insert
//chatMapper.insertRoomMember(newRoomId, user1);
//chatMapper.insertRoomMember(newRoomId, user2);
//log.info("roomId={}  user1 : {}, user2 : {} 추가", newRoomId, user1, user2);
//
//return newRoomId;
