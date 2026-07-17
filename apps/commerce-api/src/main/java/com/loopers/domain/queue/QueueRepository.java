package com.loopers.domain.queue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface QueueRepository {

	boolean enqueue(Long userId);

	Long rank(Long userId);

	Long size();

	boolean exists(Long userId);

	List<Long> popWaitingUsers(int count);

	String issueAdmissionToken(Long userId, Duration ttl);

	Optional<String> getAdmissionToken(Long userId);

	boolean validateAdmissionToken(Long userId, String token);

	void deleteAdmissionToken(Long userId);
}
