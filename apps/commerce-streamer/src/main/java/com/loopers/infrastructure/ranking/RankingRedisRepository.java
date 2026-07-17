package com.loopers.infrastructure.ranking;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RankingRedisRepository {

    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration TTL = Duration.ofDays(2);

    private final RedisTemplate<String, String> redisTemplate;

    public RankingRedisRepository(
        @Qualifier("masterRedisTemplate") RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public void addScore(Long productId, double score, ZonedDateTime occurredAt) {
        String key = buildKey(occurredAt);
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), score);
        redisTemplate.expire(key, TTL);
    }

    private String buildKey(ZonedDateTime occurredAt) {
        String date = occurredAt.withZoneSameInstant(ZONE).format(KEY_DATE_FORMAT);
        return KEY_PREFIX + date;
    }
}
