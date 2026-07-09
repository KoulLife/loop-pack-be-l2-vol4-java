package com.loopers.application.queue;

import java.time.Duration;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.domain.queue.QueueRepository;
import com.loopers.support.config.QueueProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대기열의 앞에서 배치 크기만큼 사용자를 꺼내(pop) 입장 토큰을 발급하는 스케줄러.
 * 처리량(= 배치 크기 / 실행 주기)만큼만 주문 API로 흘려보내 시스템을 보호한다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class QueueAdmissionScheduler {

	private final QueueRepository queueRepository;
	private final QueueProperties queueProperties;

	@Scheduled(fixedDelayString = "${queue.admission.interval-ms:1000}")
	public void admit() {
		int batchSize = queueProperties.admission().batchSize();
		List<Long> admittedUsers = queueRepository.popWaitingUsers(batchSize);

		if (admittedUsers.isEmpty()) {
			return;
		}

		Duration ttl = Duration.ofSeconds(queueProperties.token().ttlSeconds());
		for (Long userId : admittedUsers) {
			queueRepository.issueAdmissionToken(userId, ttl);
		}

		log.info("[QueueAdmission] 입장 토큰 {}건 발급 (batchSize={})", admittedUsers.size(), batchSize);
	}
}
