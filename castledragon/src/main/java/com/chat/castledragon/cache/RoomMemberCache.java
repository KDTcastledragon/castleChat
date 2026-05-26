package com.chat.castledragon.cache;

import java.util.Set;
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

	public void cacheRoomMembers(Long roomId, Set<Long> memberIds) {
		String key = rmSizeKey(roomId);

		Set<String> values = memberIds.stream().map(String::valueOf).collect(Collectors.toSet());

		redisTemplate.opsForSet().add(key, values.toArray(new String[0]));
	}

	// **Java 문법이 아니라 Redis에 저장할 “key 이름 규칙”**이야.
	// 직접 ChatService나 ChatHandler 여기저기에 문자열로 쓰면 나중에 지옥문이 열려. 그래서 보통 Redis 전용 클래스를 하나 만든다.

	//	cd D:\castleDragonProjects\castleChat\castledragon .\gradlew.bat clean compileJava --refresh-dependencies

	//	IDE에서 castledragon 프로젝트를 Gradle Refresh
	//	Eclipse/STS면 castledragon 우클릭 → Gradle → Refresh Gradle Project
	//
	//	그래도 안 되면 Project Clean
	//	Project → Clean
	//
	//	VS Code라면
	//	Ctrl + Shift + P → Java: Clean Java Language Server Workspace
	//
	//	IntelliJ라면
	//	Gradle 탭 → Reload All Gradle Projects
}
