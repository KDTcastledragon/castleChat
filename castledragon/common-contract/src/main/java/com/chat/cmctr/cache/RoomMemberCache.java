package com.chat.cmctr.cache;

import java.time.Duration;
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
	private final StringRedisTemplate redisTemplate; // redisTemplate란? Spring이 제공하는 Redis 조작하는 명령 실행 도구야. DB에서 MyBatis Mapper가 SQL 실행 도구인 느낌.
	//	private final RedisTemplate<String, Long> redisTemplate; // 가능은 한데, 쓰기 개빡세다. 
	//	Redis는 내부적으로 byte[] 저장소라서, Java의 Long을 저장하려면 serializer 설정이 필요함. 아래의 @Bean 예시가 그러하다 --->
	//	@Bean
	//	public RedisTemplate<String, Long> longRedisTemplate(RedisConnectionFactory connectionFactory) {
	//	    RedisTemplate<String, Long> template = new RedisTemplate<>();
	//	    template.setConnectionFactory(connectionFactory);
	//
	//	    template.setKeySerializer(new StringRedisSerializer());
	//	    template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
	//
	//	    template.afterPropertiesSet();
	//	    return template;
	//	}

	private static final Duration ROOM_MEMBERS_TTL = Duration.ofHours(48);
	private static final Duration ROOM_MEMBERS_TTL_REFRESH_THRESHOLD = Duration.ofHours(1);

	public RoomMemberCache(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	// key Name make function 이당. 왜 함수로 빼냐? 여기저기서 직접 문자열을 만들면 위험해. 오타 나면 Redis key가 갈라져버림. 그래서 key 규칙을 한 메소드에 고정한 거야.
	private String roomMembersKey(Long roomId) {
		return "chat:room:" + roomId + ":members";
		// **Java 문법이 아니라 Redis에 저장할 “key 이름 규칙”**이야.
		// 직접 ChatService나 ChatHandler 여기저기에 문자열로 쓰면 나중에 지옥문이 열려. 그래서 보통 Redis 전용 클래스를 하나 만든다.
	}

	private void refreshRoomMembersTtlIfNeeded(String key) {
		Long ttlSeconds = redisTemplate.getExpire(key);

		if (ttlSeconds == null) {
			return;
		}

		// -2: key 없음, -1: 만료시간 없음
		if (ttlSeconds < 0) {
			return;
		}

		if (ttlSeconds <= ROOM_MEMBERS_TTL_REFRESH_THRESHOLD.getSeconds()) {
			redisTemplate.expire(key, ROOM_MEMBERS_TTL);
			log.info("Redis roomMembers TTL 연장. key={}, ttlSeconds={}", key, ttlSeconds);
		}
	}

	//====== Redis에 방 멤버 Set 저장 ======================================================================================
	public String initOrReplaceRoomMembers(Long roomId, Set<Long> memberUserIds) {
		String key = roomMembersKey(roomId); // 1. roomId로 Redis key 이름 생성

		redisTemplate.delete(key); // 2. Redis 안에서 key 이름과 일치하는 데이터 전체를 삭제. key에 예전 멤버 Set이 있으면 삭제. 
		// DEL chat:room:key:members 와 같다. chat:room:2:members -> Set {1, 2, 3} 이 데이터를 한방에 그냥 delete 시키는거다. chat:room:2:members -> null이 된다.
		// redisTemplate.delete("chat:room:*:members"); // 이런 식으로 전체 삭제 시도 X. 인생 망함.

		// 왜 이걸 먼저 하냐? SADD는 “교체”가 아니라 “추가”야.
		// e.g.) chat:room:2:members = {"1", "2", "3", "4", "5"} -> SADD chat:room:2:members 1 2 -> {"1", "2", "3", "4", "5"} 그대로다. 3,4,5 지워지지 않음.
		// 그래서, DEL chat:room:2:members -> SADD chat:room:2:members 1 2 -> chat:room:2:members = {"1", "2"} 흐름으로 가는 거다.
		// 현재 메소드는 “멤버 일부 변경용” 메소드가 아니라, DB 기준으로 Redis 캐시를 통째로 재동기화하는 메소드야. === DB에서 확인한 최신 전체 멤버 목록이 1,2,3,5니까 Redis도 이 상태로 완전히 맞춰라.

		if (memberUserIds == null || memberUserIds.isEmpty()) {
			log.info("roomMembers 비어있음. key={} 삭제 후 저장 생략", key);
			return key;
		}

		Set<String> values = memberUserIds.stream().map(String::valueOf).collect(Collectors.toSet()); // 3. 새 memberIds를 String Set으로 변환
		// Set<Long> memberUserIds = [1L, 2L, 3L] --> Set<String> values = ["1", "2", "3"]
		// .map(String::valueOf) : .map(id -> String.valueOf(id))와 같다.
		// .collect(Collectors.toSet()) : Stream에 흐르는 값들을 Set으로 모아라. --> Set<String> values = Set.of("1", "2", "3");

		redisTemplate.opsForSet().add(key, values.toArray(new String[0])); // 4. Redis에 새로 저장 === SADD chat:room:2:members 1 2 3 4 5
		// .add는 한번에 values들을 넘길 수 있다. : .add(key, "1", "2", "3", ...); 단, .add(key, values); 는 안된다.
		// values.toArray(new String[0]) : values(지금은 Set<String>)를 String[] 배열로 바꿔라.
		// e.g.) Set<String> values = Set.of("1", "2", "3"); --> String[] arr = values.toArray(new String[0]); --> arr = new String[] {"1", "2", "3"};
		// String[0] : 0칸짜리 배열을 넘겨도, Java가 내부에서 필요한 크기의 새 배열을 만들어 반환해. values.toArray(String[]::new) 도 가능함.
		// values.size() = 3이고 new String[0] = 길이 0이어도 , new String[] {"1", "2", "3"} 해줌.
		// new String[] {"1", "2", "3"} : 이건 Java 배열 생성 문법이야. new String[] ["1", "2", "3"] 이거 아니다. 주의해야됨.
		// JavaScript에서는 [1,2,3] 표시하지만, Java는 배열 값을 {} 중괄호로 초기화해.  e.g.) String[] arr = { "1", "2", "3" };

		redisTemplate.expire(key, ROOM_MEMBERS_TTL);

		// --> 전체구조 : 선 삭제 -> 후 추가.  “정확성 우선” 방식이기 때문에 아래 4가지의 경우에만 사용한다. 함부로 쓰면 꼬인다.

		// < 이 메소드를 쓰는 경우 >
		// 1. 방 최초 생성 직후 
		// 2. Redis에 캐시가 없어서 DB에서 전체 active 멤버를 조회한 직후
		// 3. 캐시가 꼬였다고 판단해서 DB 기준으로 강제 재동기화할 때 
		// 4. 방 비활성화/삭제 처리할 때

		return key;
	}

	//======  Redis에서 방 멤버 Set 조회 ======================================================================================
	public Set<Long> getRoomMembers(Long roomId) {
		String key = roomMembersKey(roomId);

		Set<String> values = redisTemplate.opsForSet().members(key);

		//		log.info("Redis 방 멤버 set 조회 : {}", values);

		if (values == null || values.isEmpty()) {
			return Set.of(); // --> Set<Long> = Set.of() --> [];
		}

		refreshRoomMembersTtlIfNeeded(key);

		return values.stream().map(Long::valueOf).collect(Collectors.toSet()); // --> Set<Long> = Set.of(1L, 2L, 3L) --> [1L,2L,3L]
	}

	// ======  Redis에 있으면 Redis 반환, 없으면 DB 조회 후 Redis 저장. lazy loading. =========================================================================
	public Set<Long> getOrLoadRoomMembers(Long roomId, Supplier<List<Long>> dbLoader) {
		Set<Long> cachedMembers = getRoomMembers(roomId);

		if (!cachedMembers.isEmpty()) {

			//			log.info("MembersRedis 존재. 저장된 Redis 반환. : {}", cachedMembers);
			return cachedMembers;
		}

		//		log.info("MembersRedis 없음. DB조회 후, Redis 저장 필요.");

		List<Long> dbMembers = dbLoader.get();

		if (dbMembers == null || dbMembers.isEmpty()) {
			return Set.of();
		}

		Set<Long> memberSet = new HashSet<>(dbMembers);

		String createdRedisKey = initOrReplaceRoomMembers(roomId, memberSet);

		//		log.info("DB조회 후, MembersRedis 저장 성공. key : {}", createdRedisKey);
		return memberSet;
	}

	// ====== Redis Set 크기 조회 ======================================================================================
	public long countRoomMembers(Long roomId) {
		String key = roomMembersKey(roomId);

		Long count = redisTemplate.opsForSet().size(key);

		if (count == null) {

			log.info("{} memberRedis null.", key);

			return 0L;
		}

		log.info("{} 방 인원수 : {}", key, count);

		return count;
	}

	public void removeRoomMember(Long roomId, Long userId) {
		String key = roomMembersKey(roomId);

		Boolean exists = redisTemplate.hasKey(key);

		if (!Boolean.TRUE.equals(exists)) {
			return;
		}

		redisTemplate.opsForSet().remove(key, String.valueOf(userId));
	}

}

//	cd D:\castleDragonProjects\castleChat\castledragon .\gradlew.bat clean compileJava --refresh-dependencies : redis gradle dependency 의존성 해결.(결과적으로 안되긴 함.)

//	IDE에서 castledragon 프로젝트를 Gradle Refresh
//	Eclipse/STS : castledragon 우클릭 → Gradle → Refresh Gradle Project
//	그래도 안 되면 : Project Clean Project → Clean
//	VS Code : Ctrl + Shift + P → Java: Clean Java Language Server Workspace
//	IntelliJ : Gradle 탭 → Reload All Gradle Projects
