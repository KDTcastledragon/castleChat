package com.chat.aiassist.support;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.chat.aiassist.exception.AiRateLimitExceededException;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class AiRecommendRateLimiter {

	private static final String OK = "OK";
	private static final long MINUTE_TTL_SECONDS = Duration.ofMinutes(1).toSeconds();
	private static final long DAY_TTL_SECONDS = Duration.ofDays(1).toSeconds();

	private static final String RATE_LIMIT_LUA = """
			local limits = {
				tonumber(ARGV[1]),
				tonumber(ARGV[2]),
				tonumber(ARGV[3]),
				tonumber(ARGV[4])
			}

			local ttls = {
				tonumber(ARGV[5]),
				tonumber(ARGV[6]),
				tonumber(ARGV[7]),
				tonumber(ARGV[8])
			}

			for i = 1, #KEYS do
				local current = tonumber(redis.call('GET', KEYS[i]) or '0')
				if current >= limits[i] then
					return KEYS[i]
				end
			end

			for i = 1, #KEYS do
				local nextValue = redis.call('INCR', KEYS[i])
				if nextValue == 1 then
					redis.call('EXPIRE', KEYS[i], ttls[i])
				end
			end

			return 'OK'
			""";

	private final StringRedisTemplate redisTemplate;
	private final DefaultRedisScript<String> rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_LUA, String.class);

	private final int userPerMinute;
	private final int userPerDay;
	private final int globalPerMinute;
	private final int globalPerDay;

	public AiRecommendRateLimiter(
			StringRedisTemplate redisTemplate,
			@Value("${ai.assist.rate-limit.user-per-minute:1}") int userPerMinute,
			@Value("${ai.assist.rate-limit.user-per-day:5}") int userPerDay,
			@Value("${ai.assist.rate-limit.global-per-minute:5}") int globalPerMinute,
			@Value("${ai.assist.rate-limit.global-per-day:80}") int globalPerDay) {
		this.redisTemplate = redisTemplate;
		this.userPerMinute = userPerMinute;
		this.userPerDay = userPerDay;
		this.globalPerMinute = globalPerMinute;
		this.globalPerDay = globalPerDay;
	}

	public void checkRecommendLimit(Long requesterUserId) {
		if (requesterUserId == null) {
			throw new IllegalArgumentException("requesterUserId가 없습니다.");
		}

		List<String> keys = List.of(
				"aiassist:recommend:user:minute:" + requesterUserId,
				"aiassist:recommend:user:day:" + requesterUserId,
				"aiassist:recommend:global:minute",
				"aiassist:recommend:global:day");

		String result = redisTemplate.execute(rateLimitScript, keys,
				String.valueOf(userPerMinute),
				String.valueOf(userPerDay),
				String.valueOf(globalPerMinute),
				String.valueOf(globalPerDay),
				String.valueOf(MINUTE_TTL_SECONDS),
				String.valueOf(DAY_TTL_SECONDS),
				String.valueOf(MINUTE_TTL_SECONDS),
				String.valueOf(DAY_TTL_SECONDS));

		if (OK.equals(result)) {
			return;
		}

		log.warn("AI 추천 rate limit 초과. requesterUserId={}, exceededKey={}", requesterUserId, result);
		throw new AiRateLimitExceededException(toLimitMessage(result));
	}

	private String toLimitMessage(String exceededKey) {
		if (exceededKey == null) {
			return "AI 추천 사용량 제한을 초과했습니다.";
		}

		if (exceededKey.contains(":user:minute:")) {
			return "AI 추천은 1분에 " + userPerMinute + "번만 사용할 수 있습니다.";
		}

		if (exceededKey.contains(":user:day:")) {
			return "AI 추천은 하루에 " + userPerDay + "번만 사용할 수 있습니다.";
		}

		if (exceededKey.contains(":global:minute")) {
			return "현재 AI 추천 요청이 몰려 잠시 후 다시 시도해주세요.";
		}

		if (exceededKey.contains(":global:day")) {
			return "오늘의 포트폴리오 AI 무료 사용량이 모두 소진되었습니다.";
		}

		return "AI 추천 사용량 제한을 초과했습니다.";
	}
}
