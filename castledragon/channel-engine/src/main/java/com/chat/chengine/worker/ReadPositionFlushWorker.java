package com.chat.chengine.worker;

import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chat.chengine.mapper.ChatMapper;
import com.chat.redis.cache.RoomReadPositionCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class ReadPositionFlushWorker {

	private final RoomReadPositionCache roomReadPositionCache;
	private final ChatMapper chatMapper;

	//	@Scheduled(fixedDelay = 5000)
	@Scheduled(fixedDelayString = "${chat.read-position.flush-delay-ms:5000}")
	@Transactional
	public void flushDirtyReadPositions() {
		Map<String, Long> dirtyReadPositions = roomReadPositionCache.getDirtyReadPositions();
		log.info("flush Worker 작동 시작. dRP : {}", dirtyReadPositions);

		if (dirtyReadPositions == null || dirtyReadPositions.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Long> entry : dirtyReadPositions.entrySet()) {
			String dirtyField = entry.getKey();
			Long lastReadMessageId = entry.getValue();

			String[] parts = dirtyField.split(":");

			if (parts.length != 2) {
				log.warn("invalid dirty read-position field: {}", dirtyField);
				continue;
			}

			Long roomId = Long.valueOf(parts[0]);
			Long userId = Long.valueOf(parts[1]);

			int updated = chatMapper.updateLastReadMessageId(roomId, userId, lastReadMessageId);

			if (updated > 0) {
				roomReadPositionCache.removeDirtyReadPosition(roomId, userId);
				log.info("flushed read-position. roomId={}, userId={}, lrm={}", roomId, userId, lastReadMessageId);
				continue;
			}

			roomReadPositionCache.removeDirtyReadPosition(roomId, userId);
			log.warn("removed stale dirty read-position. roomId={}, userId={}, lrm={} - active room_member not found", roomId, userId, lastReadMessageId);
		}
	}
}
