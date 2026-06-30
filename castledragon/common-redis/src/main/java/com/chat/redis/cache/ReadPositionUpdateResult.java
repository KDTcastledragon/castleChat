package com.chat.redis.cache;

public record ReadPositionUpdateResult(boolean updated, Long oldLastReadMessageId, Long newLastReadMessageId) {
}