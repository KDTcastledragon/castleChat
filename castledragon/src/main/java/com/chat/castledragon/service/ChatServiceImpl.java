package com.chat.castledragon.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.castledragon.cache.RoomMemberCache;
import com.chat.castledragon.domain.ChatMemberDTO;
import com.chat.castledragon.domain.ChatMessageDTO;
import com.chat.castledragon.domain.ChatMessageResponseDTO;
import com.chat.castledragon.domain.ChatRoomDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.EnterGroupResponseDTO;
import com.chat.castledragon.domain.EnterRoomResponseDTO;
import com.chat.castledragon.domain.PayloadSendMessageDTO;
import com.chat.castledragon.domain.SessionUserDTO;
import com.chat.castledragon.domain.UserProfileResponseDTO;
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

	private Long createRoomAndGetRoomId(String roomType, String roomStatus, String roomName) {
		ChatRoomDTO room = new ChatRoomDTO();
		room.setRoomType(roomType);
		room.setRoomStatus(roomStatus);
		room.setRoomName(roomName);

		chatMapper.createRoom(room); // dto 방식에서, parameter 방식으로 변경.하려 했으나,,, 결국 다시 왔소.
		// chatMapper.createRoom("DIRECT", "ACTIVE");  // generated roomId를 받을 곳이 없음. 그래서 안 씀. ...
		// 사실, 뭐 CreateRoomParam Helper Object를 만들수도 있기는 한데, 그래봐야 어차피 param.set~() --> param.getRoomId(); 해야되서 결국 조삼모사....

		Long roomId = room.getRoomId();

		return roomId;
	}

	@Override
	@Transactional
	public EnterRoomResponseDTO enterDirectRoom(Long senderUserId, String senderNickname, String friendPublicId) {

		//  Long friendUserId = userMapper.findUserInfoByPublicId(friendPublicId).getUserId(); // 이건 DB 조회가 2번 나갈 가능성이 커. 같은 유저를 두 번 찾는 거라 낭비야.
		//  String friendNickname = userMapper.findUserInfoByPublicId(friendPublicId).getNickname(); // 이건 DB 조회가 2번 나갈 가능성이 커. 같은 유저를 두 번 찾는 거라 낭비야.
		ChatMemberDTO friend = userMapper.findUserInfoByPublicId(friendPublicId);

		if (friend == null) {
			return null;
		}

		Long friendUserId = friend.getUserId();
		String friendNickname = friend.getNickname();

		if (friendUserId == null) {
			return null;
		}

		// 1. 기존 room 조회
		Long roomId = chatMapper.findRoomId(senderUserId, friendUserId);
		boolean isNewRoom = false;

		if (roomId == null) {
			log.info("새로운 채팅방(roomId) 생성 시작");

			roomId = createRoomAndGetRoomId("DIRECT", "ACTIVE", senderUserId + "_" + friendUserId + "_DIRECT");

			log.info("새로운 채팅방의 newRoomId 생성 완료 : {}", roomId);

			chatMapper.insertRoomMember(roomId, senderUserId, "MEMBER", friendNickname + "님과의 채팅방");
			chatMapper.insertRoomMember(roomId, friendUserId, "MEMBER", senderNickname + "님과의 채팅방");

			roomMemberCache.cacheRoomMembers(roomId, Set.of(senderUserId, friendUserId));

			isNewRoom = true;

			log.info("roomId={}  user1 : {}, user2 : {} 추가", roomId, senderUserId, friendUserId);
		}

		if (!isNewRoom) {
			Long finalRoomId = roomId;
			roomMemberCache.getOrLoadRoomMembers(finalRoomId, () -> chatMapper.findActiveRoomMemberIds(finalRoomId));
			// 여기서 finalRoomId를 쓰는 이유는 람다 안에서 사용하는 지역 변수는 Java에서 effectively final이어야 하기 때문이야. 
			// roomId는 위에서 값이 바뀌었으니 람다 안에서 바로 쓰면 오류가 날 수 있어.
		}

		// 2. 메시지 조회. --> if 다음 else 안 쓰는 이유? 1. nesting(중첩)때문에.가독성저하.조건흐름추적어려움.  2.Pyramid of Doom형태로 else의 else의 else...가 되버리기 때문.
		log.info("{}과 {}의 채팅방 이미 존재. : {}  --> 이전 채팅내역 불러오기 시작.", senderUserId, friendUserId, roomId);
		//		List<ChatDTO> messages = chatMapper.getMessages(roomId);

		// 3. DTO 조립
		EnterRoomResponseDTO resDTO = new EnterRoomResponseDTO();
		String targetUserLoginId = userMapper.getUserLoginId(friendUserId);
		log.info("채팅방 상대의 로그인ID : {}", targetUserLoginId);

		resDTO.setRoomId(roomId);
		//				resDTO.setMessages(messages);
		resDTO.setTargetUserId(friendUserId);
		resDTO.setTargetLoginId(targetUserLoginId);

		log.info("최종 resDTO : {}", resDTO);

		return resDTO;
	}

	@Override
	@Transactional
	public ChatMessageResponseDTO sendMessage(Long senderUserId, String senderPublicId, PayloadSendMessageDTO payload, Set<Long> viewingUserIds) {

		Long roomId = payload.getRoomId();

		ChatMessageDTO sendChat = new ChatMessageDTO();
		sendChat.setRoomId(roomId);
		sendChat.setSenderId(senderUserId);
		sendChat.setMessageText(payload.getMessageText());

		chatMapper.insertMessage(sendChat); // DB에 Msg 저장.

		// 방 멤버 전체를 Redis에서 가져온다. 없으면 DB에서 가져와 Redis에 올린다.
		Set<Long> totalRoomMemberIds = roomMemberCache.getOrLoadRoomMembers(roomId, () -> chatMapper.findActiveRoomMemberIds(roomId));

		// 현재 방을 보고 있는 사람들은 메시지를 즉시 받은 상태니까 읽은 사람으로 본다. 보낸 사람도 자기 메시지는 당연히 읽은 상태니까 추가한다.
		Set<Long> connectedRoomMemberIds = new HashSet<>(viewingUserIds);
		connectedRoomMemberIds.add(senderUserId);

		connectedRoomMemberIds.retainAll(totalRoomMemberIds); // 혹시 이상한 userId가 섞였더라도 실제 방 멤버만 남긴다.

		log.info("{}방의 총 유저 : {}  , 현재 연결된 유저 : {}", roomId, totalRoomMemberIds, connectedRoomMemberIds);

		Long unreadCount = (long) (totalRoomMemberIds.size() - connectedRoomMemberIds.size()); // 안 읽은 사람 수 계산.

		log.info("unreadCount : {}", unreadCount);

		if (unreadCount < 0) {
			unreadCount = 0L; // === (long) 0;
		}

		ChatMessageResponseDTO resChat = new ChatMessageResponseDTO();

		resChat.setMessageId(sendChat.getMessageId());
		resChat.setRoomId(sendChat.getRoomId());
		resChat.setSenderPublicId(senderPublicId);
		resChat.setMessageText(sendChat.getMessageText());
		resChat.setCreatedAt(sendChat.getCreatedAt());
		resChat.setUnreadCount(unreadCount);

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
	public EnterGroupResponseDTO createGroupRoom(SessionUserDTO host, String roomName, List<String> selectedFriendPublicIdList) {

		if (roomName == null || roomName.trim().isEmpty()) {
			roomName = host.getNickname() + "님의 단톡방";
		}

		Long roomId = createRoomAndGetRoomId("GROUP", "ACTIVE", roomName);

		List<ChatMemberDTO> groupRoomMemberList = new ArrayList<>(); // UserProfileResponseDTO 대신, 내부 Mappiing용으로 GroupRoomMemberDTO 타입 생성 .

		// 1. pubId로 userId찾아서 insert를 위한 profileInfo getting.

		// 방장 먼저 넣어줌.
		ChatMemberDTO hostInfo = ChatMemberDTO.from(host); // 정적 팩토리 메소드(static factory method). 자세한 설명은 chatMdto ㄱㄱ.
		groupRoomMemberList.add(hostInfo);

		for (String pubId : selectedFriendPublicIdList) {
			ChatMemberDTO memberInfo = userMapper.findUserInfoByPublicId(pubId);
			// insertList.add(member); // 여기서 추가 안하고 밑의 if문에서 추가.

			if (memberInfo != null) {
				groupRoomMemberList.add(memberInfo);
			}

		} // groupRoomMemberList에 ChatMemberDTO 형태로 모든 단톡방 멤버 정보 add.

		// 2. gRML에서 userId만 추출하여 insert 사전 작업 하기.
		Set<Long> groupRoomMemberUserIdSet = new LinkedHashSet<>();
		//		groupRoomMemberUserIdSet.add(hostInfo.getUserId()); // 이미 groupRoomMemberList에 .add(hostInfo)했기 때문에, 중벅이라 안 써도딘당.

		for (ChatMemberDTO member : groupRoomMemberList) {
			groupRoomMemberUserIdSet.add(member.getUserId());
		}

		// 3. insert
		for (Long id : groupRoomMemberUserIdSet) {
			String role = id.equals(host.getUserId()) ? "HOST" : "MEMBER";
			chatMapper.insertRoomMember(roomId, id, role, roomName);
		}

		// 단톡방 session
		roomMemberCache.cacheRoomMembers(roomId, groupRoomMemberUserIdSet);

		// 4. response Data 조립
		List<UserProfileResponseDTO> responseMemberList = new ArrayList<>();

		for (ChatMemberDTO m : groupRoomMemberList) {
			responseMemberList.add(new UserProfileResponseDTO(m.getPublicId(), m.getNickname(), m.getFriendCode(), m.getProfileImg()));
		}

		Long roomMemberCount = (long) responseMemberList.size();

		EnterGroupResponseDTO resData = new EnterGroupResponseDTO(roomId, roomName, roomMemberCount, responseMemberList);

		log.info("GroupCreated : {} {} {} {}", roomId, roomName, roomMemberCount, responseMemberList);

		return resData;

	}// createGroupRoom

	@Override
	public List<ChatRoomListDTO> getMyAllRooms(Long userId) {
		List<ChatRoomListDTO> list = chatMapper.getMyChatRooms(userId);
		return list;
	}

}//serviceImpl
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
