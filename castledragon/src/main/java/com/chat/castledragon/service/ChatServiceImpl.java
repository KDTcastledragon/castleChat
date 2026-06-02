package com.chat.castledragon.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.castledragon.cache.RoomMemberCache;
import com.chat.castledragon.domain.ChatMessageDTO;
import com.chat.castledragon.domain.ChatRoomListDTO;
import com.chat.castledragon.domain.ChatRoomsDTO;
import com.chat.castledragon.domain.EnterRoomResponseDTO;
import com.chat.castledragon.domain.PayloadSendMessageDTO;
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

	@Override
	@Transactional
	public EnterRoomResponseDTO enterDirectRoom(Long senderId, String friendPublicId) {

		Long friendUserId = userMapper.findUserIdByPublicId(friendPublicId);

		if (friendUserId == null) {
			return null;
		}

		// 1. 기존 room 조회
		Long roomId = chatMapper.findRoomId(senderId, friendUserId);
		boolean isNewRoom = false;

		if (roomId == null) {
			log.info("새로운 채팅방(roomId) 생성 시작");
			ChatRoomsDTO room = new ChatRoomsDTO();
			room.setRoomType("DIRECT");
			room.setRoomStatus("ACTIVE");

			chatMapper.createRoom(room);
			roomId = room.getRoomId();

			log.info("새로운 채팅방의 newRoomId 생성 완료 : {}", roomId);

			chatMapper.insertRoomMember(roomId, senderId);
			chatMapper.insertRoomMember(roomId, friendUserId);

			roomMemberCache.cacheRoomMembers(roomId, Set.of(senderId, friendUserId));

			isNewRoom = true;

			log.info("roomId={}  user1 : {}, user2 : {} 추가", roomId, senderId, friendUserId);
		}

		if (!isNewRoom) {
			Long finalRoomId = roomId;
			roomMemberCache.getOrLoadRoomMembers(finalRoomId, () -> chatMapper.findActiveRoomMemberIds(finalRoomId));
			// 여기서 finalRoomId를 쓰는 이유는 람다 안에서 사용하는 지역 변수는 Java에서 effectively final이어야 하기 때문이야. 
			// roomId는 위에서 값이 바뀌었으니 람다 안에서 바로 쓰면 오류가 날 수 있어.
		}

		// 2. 메시지 조회. --> if 다음 else 안 쓰는 이유? 1. nesting(중첩)때문에.가독성저하.조건흐름추적어려움.  2.Pyramid of Doom형태로 else의 else의 else...가 되버리기 때문.
		log.info("{}과 {}의 채팅방 이미 존재. : {}  --> 이전 채팅내역 불러오기 시작.", senderId, friendUserId, roomId);
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
	public ChatMessageDTO sendMessage(Long senderId, PayloadSendMessageDTO payload, Set<Long> viewingUserIds) {

		Long roomId = payload.getRoomId();

		ChatMessageDTO chat = new ChatMessageDTO();
		chat.setRoomId(roomId);
		chat.setSenderId(senderId);
		chat.setMsgText(payload.getMsgText());

		chatMapper.insertMessage(chat); // DB에 Msg 저장.

		// 방 멤버 전체를 Redis에서 가져온다. 없으면 DB에서 가져와 Redis에 올린다.
		Set<Long> totalRoomMemberIds = roomMemberCache.getOrLoadRoomMembers(roomId, () -> chatMapper.findActiveRoomMemberIds(roomId));

		// 현재 방을 보고 있는 사람들은 메시지를 즉시 받은 상태니까 읽은 사람으로 본다. 보낸 사람도 자기 메시지는 당연히 읽은 상태니까 추가한다.
		Set<Long> connectedRoomMemberIds = new HashSet<>(viewingUserIds);
		connectedRoomMemberIds.add(senderId);

		connectedRoomMemberIds.retainAll(totalRoomMemberIds); // 혹시 이상한 userId가 섞였더라도 실제 방 멤버만 남긴다.

		log.info("{}방의 총 유저 : {}  , 현재 연결된 유저 : {}", roomId, totalRoomMemberIds, connectedRoomMemberIds);

		Long unreadCount = (long) (totalRoomMemberIds.size() - connectedRoomMemberIds.size()); // 안 읽은 사람 수 계산.

		log.info("unreadCount : {}", unreadCount);

		if (unreadCount < 0) {
			unreadCount = (long) 0;
		}

		chat.setUnreadCount(unreadCount);

		return chat;
	}

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

	@Override
	public List<ChatMessageDTO> getMessages(Long roomId) {
		List<ChatMessageDTO> chatList = chatMapper.getMessages(roomId);
		return chatList;
	}

	@Override
	public void updateLastRead(Long roomId, Long userId, Long lastReadMessageId) {
		chatMapper.updateLastRead(roomId, userId, lastReadMessageId);
	}

	@Override
	public List<ChatRoomListDTO> getMyChatRooms(Long userId) {
		return chatMapper.getMyChatRooms(userId);
	}

}

//if (roomId != null) {
//	log.info("{}과 {}의 채팅방 이미 존재 : {}", user1, user2, roomId);
//	return roomId;
//}
////이걸 Guard Clause(가드 절) 또는 Early Return 패턴이라고 한다. if로 방어 조건을 먼저 빼는 것.
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
