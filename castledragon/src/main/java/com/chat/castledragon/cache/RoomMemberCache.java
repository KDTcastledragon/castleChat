package com.chat.castledragon.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class RoomMemberCache {
	private final StringRedisTemplate redisTemplate;

	public RoomMemberCache(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	private String rmSizeKey(Long roomId) {
		return "chat:room:" + roomId + ":members";
	}

	//====== Redis에 방 멤버 Set 저장 ======================================================================================
	public void cacheRoomMembers(Long roomId, Set<Long> memberIds) {
		String key = rmSizeKey(roomId);

		Set<String> values = memberIds.stream().map(String::valueOf).collect(Collectors.toSet());

		redisTemplate.opsForSet().add(key, values.toArray(new String[0]));
	}

	//======  Redis에서 방 멤버 Set 조회 ======================================================================================
	public Set<Long> getRoomMembers(Long roomId) {
		String key = rmSizeKey(roomId);

		Set<String> values = redisTemplate.opsForSet().members(key);

		log.info("Redis 방 멤버 set 조회 : {}", values);

		if (values == null || values.isEmpty()) {
			return Set.of();
		}

		return values.stream().map(Long::valueOf).collect(Collectors.toSet());
	}

	// ======  Redis에 있으면 Redis 반환, 없으면 DB 조회 후 Redis 저장 =========================================================================
	public Set<Long> getOrLoadRoomMembers(Long roomId, Supplier<List<Long>> dbLoader) {
		Set<Long> cachedMembers = getRoomMembers(roomId);

		log.info("Redis cachedMembers 조회 : {}", cachedMembers);

		if (!cachedMembers.isEmpty()) {

			log.info("Redis cachedMembers 조회 : {}", cachedMembers);
			return cachedMembers;
		}

		List<Long> dbMembers = dbLoader.get();

		if (dbMembers == null || dbMembers.isEmpty()) {
			return Set.of();
		}

		Set<Long> memberSet = new HashSet<>(dbMembers);

		cacheRoomMembers(roomId, memberSet);

		log.info("Redis memberSet 조회 : {}", memberSet);
		return memberSet;
	}

	// ====== Redis Set 크기 조회 ======================================================================================
	public long countRoomMembers(Long roomId) {
		String key = rmSizeKey(roomId);

		Long count = redisTemplate.opsForSet().size(key);

		if (count == null) {

			log.info("Redis countRoomMembers 조회 : {}", count);

			return 0L;
		}

		log.info("Redis countRoomMembers 조회 : {}", count);

		return count;
	}

	// **Java 문법이 아니라 Redis에 저장할 “key 이름 규칙”**이야.
	// 직접 ChatService나 ChatHandler 여기저기에 문자열로 쓰면 나중에 지옥문이 열려. 그래서 보통 Redis 전용 클래스를 하나 만든다.

	//	cd D:\castleDragonProjects\castleChat\castledragon .\gradlew.bat clean compileJava --refresh-dependencies : redis gradle dependency 의존성 해결.(결과적으로 안되긴 함.)

	//	IDE에서 castledragon 프로젝트를 Gradle Refresh
	//	Eclipse/STS : castledragon 우클릭 → Gradle → Refresh Gradle Project
	//	그래도 안 되면 : Project Clean Project → Clean
	//	VS Code : Ctrl + Shift + P → Java: Clean Java Language Server Workspace
	//	IntelliJ : Gradle 탭 → Reload All Gradle Projects
}
