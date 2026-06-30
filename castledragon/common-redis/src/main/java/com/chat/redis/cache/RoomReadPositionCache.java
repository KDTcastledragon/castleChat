package com.chat.redis.cache;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class RoomReadPositionCache {

	private final StringRedisTemplate redisTemplate;

	private static final Duration READ_POSITION_TTL = Duration.ofHours(6);

	private static final String UPDATE_IF_GREATER_SCRIPT = """
			local key = KEYS[1]
			local field = ARGV[1]
			local newValue = tonumber(ARGV[2])
			local fallbackOldValue = tonumber(ARGV[3])
			local ttlSeconds = tonumber(ARGV[4])

			local currentValue = redis.call('HGET', key, field)

			local oldValue
			if currentValue == false then
			    oldValue = fallbackOldValue
			else
			    oldValue = tonumber(currentValue)
			end

			if oldValue == nil then
			    oldValue = 0
			end

			if newValue <= oldValue then
			    redis.call('EXPIRE', key, ttlSeconds)
			    return {0, oldValue, oldValue}
			end

			redis.call('HSET', key, field, newValue)
			redis.call('EXPIRE', key, ttlSeconds)

			return {1, oldValue, newValue}
			""";

	private String readPositionKey(Long roomId) {
		return "chat:room:" + roomId + ":read-position";
	}

	private String dirtyReadPositionKey() {
		return "chat:dirty:read-position";
	}

	private String dirtyField(Long roomId, Long userId) {
		return roomId + ":" + userId;
	}

	public ReadPositionUpdateResult updateIfGreater(Long roomId, Long userId, Long newLastReadMessageId, Supplier<Long> dbLoader) {
		Long fallbackOldLastReadMessageId = loadFallbackOldLastReadMessageId(roomId, userId, dbLoader);

		DefaultRedisScript<List> script = new DefaultRedisScript<>();
		script.setScriptText(UPDATE_IF_GREATER_SCRIPT);
		script.setResultType(List.class);

		List<?> result = redisTemplate.execute(script, List.of(readPositionKey(roomId)), String.valueOf(userId), String
				.valueOf(newLastReadMessageId), String.valueOf(fallbackOldLastReadMessageId), String.valueOf(READ_POSITION_TTL.getSeconds()));

		if (result == null || result.size() < 3) {
			throw new IllegalStateException("Redis read-position update result invalid");
		}

		boolean updated = toLong(result.get(0)) == 1L;
		Long oldLrm = toLong(result.get(1));
		Long newLrm = toLong(result.get(2));

		if (updated) {
			markDirty(roomId, userId, newLrm);
		}

		return new ReadPositionUpdateResult(updated, oldLrm, newLrm);
	}

	private Long loadFallbackOldLastReadMessageId(Long roomId, Long userId, Supplier<Long> dbLoader) {
		Object cached = redisTemplate.opsForHash().get(readPositionKey(roomId), String.valueOf(userId));

		if (cached != null) {
			return Long.valueOf(String.valueOf(cached));
		}

		Long dbValue = dbLoader == null ? null : dbLoader.get();

		return dbValue == null ? 0L : dbValue;
	}

	public Long getLastReadMessageId(Long roomId, Long userId) {
		Object value = redisTemplate.opsForHash().get(readPositionKey(roomId), String.valueOf(userId));

		if (value == null) {
			return null;
		}

		return Long.valueOf(String.valueOf(value));
	}

	public void markDirty(Long roomId, Long userId, Long lastReadMessageId) {
		redisTemplate.opsForHash().put(dirtyReadPositionKey(), dirtyField(roomId, userId), String.valueOf(lastReadMessageId));
	}

	public void removeReadPosition(Long roomId, Long userId) {
		redisTemplate.opsForHash().delete(readPositionKey(roomId), String.valueOf(userId));
		redisTemplate.opsForHash().delete(dirtyReadPositionKey(), dirtyField(roomId, userId));
	}

	private Long toLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}

		return Long.valueOf(String.valueOf(value));
	}
}