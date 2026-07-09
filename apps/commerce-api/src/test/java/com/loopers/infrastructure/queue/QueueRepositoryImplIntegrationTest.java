package com.loopers.infrastructure.queue;

import java.time.Duration;
import java.util.List;

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
 * 대기열 Redis 저장소 통합 테스트.
 * interval-ms를 크게 두어 입장 스케줄러가 테스트 도중 자동 실행되어 큐를 비우는 것을 방지한다.
 */
@Tag("integration")
@SpringBootTest(properties = "queue.admission.interval-ms=600000")
class QueueRepositoryImplIntegrationTest {

	@Autowired private QueueRepository queueRepository;
	@Autowired private RedisCleanUp redisCleanUp;

	@AfterEach
	void tearDown() {
		redisCleanUp.truncateAll();
	}

	@DisplayName("대기열 진입 - ")
	@Nested
	class Enqueue {

		@DisplayName("진입에 성공하면 순번(0-based rank)과 전체 인원이 진입 순서대로 반영된다.")
		@Test
		void enqueue_reflectsOrderAndSize() {
			assertThat(queueRepository.enqueue(1L)).isTrue();
			assertThat(queueRepository.enqueue(2L)).isTrue();
			assertThat(queueRepository.enqueue(3L)).isTrue();

			assertThat(queueRepository.rank(1L)).isZero();
			assertThat(queueRepository.rank(2L)).isEqualTo(1L);
			assertThat(queueRepository.rank(3L)).isEqualTo(2L);
			assertThat(queueRepository.size()).isEqualTo(3L);
		}

		@DisplayName("같은 userId가 다시 진입하면 중복 진입이 거부되고 순번/전체 인원이 유지된다.")
		@Test
		void enqueue_rejectsDuplicate() {
			assertThat(queueRepository.enqueue(1L)).isTrue();
			assertThat(queueRepository.enqueue(1L)).isFalse();

			assertThat(queueRepository.rank(1L)).isZero();
			assertThat(queueRepository.size()).isEqualTo(1L);
		}

		@DisplayName("진입하지 않은 사용자의 순번 조회는 null, 존재 여부는 false다.")
		@Test
		void rank_isNull_whenNotEntered() {
			assertThat(queueRepository.rank(999L)).isNull();
			assertThat(queueRepository.exists(999L)).isFalse();
		}
	}

	@DisplayName("대기열 입장(pop) - ")
	@Nested
	class Pop {

		@DisplayName("앞에서부터 진입 순서대로 N명을 꺼내고, 꺼낸 인원은 큐에서 제거된다.")
		@Test
		void popWaitingUsers_returnsInFifoOrderAndRemoves() {
			queueRepository.enqueue(10L);
			queueRepository.enqueue(20L);
			queueRepository.enqueue(30L);

			List<Long> popped = queueRepository.popWaitingUsers(2);

			assertThat(popped).containsExactly(10L, 20L);
			assertThat(queueRepository.size()).isEqualTo(1L);
			assertThat(queueRepository.rank(30L)).isZero();
			assertThat(queueRepository.exists(10L)).isFalse();
		}

		@DisplayName("대기 인원보다 큰 수를 요청해도 있는 만큼만 꺼내며, 빈 큐면 빈 리스트를 반환한다.")
		@Test
		void popWaitingUsers_handlesUnderflowAndEmpty() {
			queueRepository.enqueue(10L);

			assertThat(queueRepository.popWaitingUsers(5)).containsExactly(10L);
			assertThat(queueRepository.popWaitingUsers(5)).isEmpty();
		}
	}

	@DisplayName("입장 토큰 - ")
	@Nested
	class AdmissionToken {

		@DisplayName("발급한 토큰은 조회/검증되고, 삭제하면 더 이상 유효하지 않다.")
		@Test
		void issue_validate_delete() {
			String token = queueRepository.issueAdmissionToken(1L, Duration.ofMinutes(5));

			assertThat(queueRepository.getAdmissionToken(1L)).contains(token);
			assertThat(queueRepository.validateAdmissionToken(1L, token)).isTrue();
			assertThat(queueRepository.validateAdmissionToken(1L, "wrong-token")).isFalse();

			queueRepository.deleteAdmissionToken(1L);

			assertThat(queueRepository.getAdmissionToken(1L)).isEmpty();
			assertThat(queueRepository.validateAdmissionToken(1L, token)).isFalse();
		}

		@DisplayName("TTL이 지나면 토큰이 자동 만료되어 검증에 실패한다.")
		@Test
		void token_expiresAfterTtl() throws InterruptedException {
			String token = queueRepository.issueAdmissionToken(1L, Duration.ofSeconds(1));
			assertThat(queueRepository.validateAdmissionToken(1L, token)).isTrue();

			Thread.sleep(1_300L);

			assertThat(queueRepository.validateAdmissionToken(1L, token)).isFalse();
			assertThat(queueRepository.getAdmissionToken(1L)).isEmpty();
		}
	}
}
