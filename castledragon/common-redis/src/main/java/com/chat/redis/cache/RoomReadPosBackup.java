package com.chat.redis.cache;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class RoomReadPosBackup {
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.
	// 백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.백업파일임.

	private final StringRedisTemplate redisTemplate;

	private static final Duration READ_POSITION_TTL = Duration.ofHours(6);
	private static final Duration READ_POSITION_TTL_REFRESH_THRESHOLD = Duration.ofMinutes(10);

	private String readPositionKey(Long roomId) {
		return "chat:room:" + roomId + ":read-position";
	}

	private String dirtyReadPositionKey() {
		return "chat:dirty:read-position";
	}

	private String dirtyField(Long roomId, Long userId) {
		return roomId + ":" + userId;
	}

	private void refreshReadPositionTtlIfNeeded(String key) {
		Long ttlSeconds = redisTemplate.getExpire(key);

		if (ttlSeconds == null || ttlSeconds < 0) {
			return;
		}

		if (ttlSeconds <= READ_POSITION_TTL_REFRESH_THRESHOLD.getSeconds()) {
			redisTemplate.expire(key, READ_POSITION_TTL);
			log.info("Redis readPosition TTL 연장. key={}, ttlSeconds={}", key, ttlSeconds);
		}
	}

	public Long getLastReadMessageId(Long roomId, Long userId) {
		String key = readPositionKey(roomId);

		Object value = redisTemplate.opsForHash().get(key, String.valueOf(userId));

		if (value == null) {
			return null;
		}

		refreshReadPositionTtlIfNeeded(key);

		return Long.valueOf(String.valueOf(value));
	}

	public void putLastReadMessageId(Long roomId, Long userId, Long lastReadMessageId) {
		String key = readPositionKey(roomId);

		redisTemplate.opsForHash().put(key, String.valueOf(userId), String.valueOf(lastReadMessageId));

		redisTemplate.expire(key, READ_POSITION_TTL);
	}

	public ReadPositionUpdateResult updateIfGreater(Long roomId, Long userId, Long newLastReadMessageId, Supplier<Long> dbLoader) {
		Long oldLastReadMessageId = getLastReadMessageId(roomId, userId);

		if (oldLastReadMessageId == null && dbLoader != null) {
			oldLastReadMessageId = dbLoader.get();

			if (oldLastReadMessageId != null) {
				putLastReadMessageId(roomId, userId, oldLastReadMessageId);
			}
		}

		Long oldValueForCompare = oldLastReadMessageId == null ? 0L : oldLastReadMessageId;

		if (oldValueForCompare >= newLastReadMessageId) {
			return new ReadPositionUpdateResult(false, oldValueForCompare, oldValueForCompare);
		}

		putLastReadMessageId(roomId, userId, newLastReadMessageId);
		markDirty(roomId, userId, newLastReadMessageId);

		return new ReadPositionUpdateResult(true, oldValueForCompare, newLastReadMessageId);
	}

	public void markDirty(Long roomId, Long userId, Long lastReadMessageId) {
		redisTemplate.opsForHash().put(dirtyReadPositionKey(), dirtyField(roomId, userId), String.valueOf(lastReadMessageId));
	}

	public void removeReadPosition(Long roomId, Long userId) {
		redisTemplate.opsForHash().delete(readPositionKey(roomId), String.valueOf(userId));
	}
}