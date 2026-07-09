package com.loopers.application.queue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.domain.queue.QueueRepository;
import com.loopers.utils.RedisCleanUp;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대기열 동시성 · 처리량 검증.
 * interval-ms=600000 → 입장 스케줄러 자동 실행 차단(입장은 admit() 직접 호출로 제어).
 * batch-size는 기본값(100)을 사용한다.
 */
@Tag("concurrency")
@SpringBootTest(properties = "queue.admission.interval-ms=600000")
class QueueConcurrencyTest {

	@Autowired private QueueApplicationService queueApplicationService;
	@Autowired private QueueAdmissionScheduler queueAdmissionScheduler;
	@Autowired private QueueRepository queueRepository;
	@Autowired private RedisCleanUp redisCleanUp;

	@AfterEach
	void tearDown() {
		redisCleanUp.truncateAll();
	}

	@DisplayName("N명이 동시에 진입해도 전원이 유실 없이, 서로 겹치지 않는 순번(0..N-1)을 배정받는다.")
	@Test
	void concurrentEnter_assignsUniqueOrderedPositions() throws InterruptedException {
		int threads = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			final long userId = i + 1L;
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					queueApplicationService.enter(userId);
				} catch (Exception ignored) {
				} finally {
					done.countDown();
				}
			});
		}

		ready.await();
		start.countDown();
		done.await();
		executor.shutdown();

		assertThat(queueRepository.size()).isEqualTo((long) threads);
		// 모든 사용자의 순번(rank)이 0..N-1 로 유일하게 배정되었는지 확인 (유실·중복 없음)
		long[] ranks = LongStream.rangeClosed(1, threads)
			.map(userId -> queueRepository.rank(userId))
			.sorted()
			.toArray();
		assertThat(ranks).isEqualTo(LongStream.range(0, threads).toArray());
	}

	@DisplayName("배치 크기(100)를 초과한 요청이 쌓여도, 한 번의 스케줄 실행은 정확히 100명만 입장시킨다.")
	@Test
	void admission_capsAtBatchSize() {
		int total = 250;
		for (long userId = 1; userId <= total; userId++) {
			queueApplicationService.enter(userId);
		}
		assertThat(queueRepository.size()).isEqualTo((long) total);

		// 1회 실행 → 100명 입장, 150명 잔류
		queueAdmissionScheduler.admit();
		assertThat(queueRepository.size()).isEqualTo(150L);
		assertThat(countIssuedTokens(total)).isEqualTo(100L);

		// 2회 실행 → 누적 200명 입장, 50명 잔류
		queueAdmissionScheduler.admit();
		assertThat(queueRepository.size()).isEqualTo(50L);
		assertThat(countIssuedTokens(total)).isEqualTo(200L);

		// 3회 실행 → 전원 입장
		queueAdmissionScheduler.admit();
		assertThat(queueRepository.size()).isZero();
		assertThat(countIssuedTokens(total)).isEqualTo((long) total);
	}

	private long countIssuedTokens(int totalUsers) {
		return LongStream.rangeClosed(1, totalUsers)
			.filter(userId -> queueRepository.getAdmissionToken(userId).isPresent())
			.count();
	}
}
