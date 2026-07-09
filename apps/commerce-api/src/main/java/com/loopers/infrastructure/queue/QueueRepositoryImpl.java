package com.loopers.infrastructure.queue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import com.loopers.domain.queue.QueueRepository;

@Repository
public class QueueRepositoryImpl implements QueueRepository {

	private static final String WAITING_QUEUE_KEY = "queue:waiting";
	private static final String WAITING_QUEUE_SEQUENCE_KEY = "queue:waiting:sequence";
	private static final String ADMISSION_TOKEN_KEY_PREFIX = "queue:admission:";

	private final RedisTemplate<String, String> redisTemplate;

	public QueueRepositoryImpl(
		@Qualifier("masterRedisTemplate") RedisTemplate<String, String> redisTemplate
	) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public boolean enqueue(Long userId) {
		Long sequence = redisTemplate.opsForValue().increment(WAITING_QUEUE_SEQUENCE_KEY);

		Boolean added = redisTemplate.opsForZSet()
			.addIfAbsent(WAITING_QUEUE_KEY, String.valueOf(userId), sequence.doubleValue());

		return Boolean.TRUE.equals(added);
	}

	@Override
	public Long rank(Long userId) {
		return redisTemplate.opsForZSet()
			.rank(WAITING_QUEUE_KEY, String.valueOf(userId));
	}

	@Override
	public Long size() {
		Long size = redisTemplate.opsForZSet().size(WAITING_QUEUE_KEY);
		return size == null ? 0L : size;
	}

	@Override
	public boolean exists(Long userId) {
		Double score = redisTemplate.opsForZSet()
			.score(WAITING_QUEUE_KEY, String.valueOf(userId));

		return score != null;
	}

	@Override
	public List<Long> popWaitingUsers(int count) {
		if (count <= 0) {
			return List.of();
		}

		Set<ZSetOperations.TypedTuple<String>> popped = redisTemplate.opsForZSet()
			.popMin(WAITING_QUEUE_KEY, count);

		if (popped == null || popped.isEmpty()) {
			return List.of();
		}

		return popped.stream()
			.map(ZSetOperations.TypedTuple::getValue)
			.filter(value -> value != null)
			.map(Long::valueOf)
			.toList();
	}

	@Override
	public String issueAdmissionToken(Long userId, Duration ttl) {
		String token = UUID.randomUUID().toString();

		redisTemplate.opsForValue()
			.set(admissionTokenKey(userId), token, ttl);

		return token;
	}

	@Override
	public Optional<String> getAdmissionToken(Long userId) {
		String token = redisTemplate.opsForValue()
			.get(admissionTokenKey(userId));

		return Optional.ofNullable(token);
	}

	@Override
	public boolean validateAdmissionToken(Long userId, String token) {
		if (token == null || token.isBlank()) {
			return false;
		}

		String savedToken = redisTemplate.opsForValue()
			.get(admissionTokenKey(userId));

		return token.equals(savedToken);
	}

	@Override
	public void deleteAdmissionToken(Long userId) {
		redisTemplate.delete(admissionTokenKey(userId));
	}

	private String admissionTokenKey(Long userId) {
		return ADMISSION_TOKEN_KEY_PREFIX + userId;
	}
}
