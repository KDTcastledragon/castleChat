package com.chat.chengine.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

/**
 * JS-safe Snowflake messageId 채번기.
 *
 * DB insert가 kafka 비동기로 넘어가면서 auto_increment를 응답에 쓸 수 없음 -> 앱에서 직접 채번.
 * 표준 snowflake(63bit)는 fe의 JS Number 안전범위(2^53-1)를 넘어 정밀도가 깨지므로 53bit로 구성.
 *
 * 구성 : [timestamp 41bit (custom epoch 기준 ms)] [workerId 5bit] [sequence 7bit]
 * - 41bit ms : 약 69년 사용 가능
 * - workerId 5bit : cheg 인스턴스 최대 32대. 인스턴스마다 chat.snowflake.worker-id 다르게 부여할 것.
 * - sequence 7bit : 노드당 1ms에 128개 = 노드당 초당 12.8만 msg.
 *
 * 시간순 단조증가 -> 기존 message_id 기반 정렬/페이징/lrm 비교 로직 그대로 사용 가능.
 */
@Component
@Log4j2
public class ChatMessageIdGenerator {

	// 2024-01-01T00:00:00Z (ms). 변경 금지. 변경하면 기존 발급 id와 시간순서가 깨진다.
	private static final long CUSTOM_EPOCH_MS = 1704067200000L;

	private static final long WORKER_ID_BITS = 5L;
	private static final long SEQUENCE_BITS = 7L;

	private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1; // 31
	private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 127

	private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
	private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

	private final long workerId;

	private long lastTimestampMs = -1L;
	private long sequence = 0L;

	public ChatMessageIdGenerator(@Value("${chat.snowflake.worker-id:0}") long workerId) {
		if (workerId < 0 || workerId > MAX_WORKER_ID) {
			throw new IllegalArgumentException("worker-id는 0~" + MAX_WORKER_ID + " 범위여야 합니다. 현재값: " + workerId);
		}

		this.workerId = workerId;
		log.info("ChatMessageIdGenerator 초기화. workerId={}", workerId);
	}

	public synchronized long nextMessageId() {
		long currentTimestampMs = System.currentTimeMillis();

		if (currentTimestampMs < lastTimestampMs) {
			// 시계 역행(NTP 보정 등). 마지막 발급 시각까지 대기하여 단조증가 보장.
			log.warn("시계 역행 감지. last={}, current={}. 대기 후 재시도.", lastTimestampMs, currentTimestampMs);
			currentTimestampMs = waitUntil(lastTimestampMs);
		}

		if (currentTimestampMs == lastTimestampMs) {
			sequence = (sequence + 1) & MAX_SEQUENCE;

			if (sequence == 0) {
				// 이번 ms의 sequence 소진 -> 다음 ms까지 대기.
				currentTimestampMs = waitUntil(lastTimestampMs + 1);
			}
		} else {
			sequence = 0L;
		}

		lastTimestampMs = currentTimestampMs;

		return ((currentTimestampMs - CUSTOM_EPOCH_MS) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
	}

	private long waitUntil(long targetTimestampMs) {
		long currentTimestampMs = System.currentTimeMillis();

		while (currentTimestampMs < targetTimestampMs) {
			currentTimestampMs = System.currentTimeMillis();
		}

		return currentTimestampMs;
	}
}
