package com.loopers.application.queue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.RedisCleanUp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 대기열 애플리케이션 서비스 통합 테스트.
 * interval-ms=600000 → 입장 스케줄러 자동 실행 차단(입장은 scheduler.admit() 직접 호출로 검증),
 * 예상 대기 시간 = ceil(순번 / 배치100) × 600초.
 */
@Tag("integration")
@SpringBootTest(properties = "queue.admission.interval-ms=600000")
class QueueApplicationServiceIntegrationTest {

	@Autowired private QueueApplicationService queueApplicationService;
	@Autowired private QueueAdmissionScheduler queueAdmissionScheduler;
	@Autowired private RedisCleanUp redisCleanUp;

	@AfterEach
	void tearDown() {
		redisCleanUp.truncateAll();
	}

	@DisplayName("대기열 진입 - ")
	@Nested
	class Enter {

		@DisplayName("최초 진입 시 순번 1, 전체 인원 1, 예상 대기 시간이 계산되어 반환된다.")
		@Test
		void firstEnter() {
			QueueEnterResult result = queueApplicationService.enter(1L);

			assertThat(result.entered()).isTrue();
			assertThat(result.position()).isEqualTo(1L);
			assertThat(result.totalWaitingCount()).isEqualTo(1L);
			assertThat(result.estimatedWaitSeconds()).isEqualTo(600L); // ceil(1/100)*600
		}

		@DisplayName("이미 진입한 사용자가 다시 진입하면 entered=false 이고 순번이 유지된다.")
		@Test
		void duplicateEnter() {
			queueApplicationService.enter(1L);
			QueueEnterResult again = queueApplicationService.enter(1L);

			assertThat(again.entered()).isFalse();
			assertThat(again.position()).isEqualTo(1L);
			assertThat(again.totalWaitingCount()).isEqualTo(1L);
		}
	}

	@DisplayName("순번 조회 - ")
	@Nested
	class GetPosition {

		@DisplayName("대기열에 진입하지 않은 사용자를 조회하면 NOT_FOUND 예외가 발생한다.")
		@Test
		void notEntered_throwsNotFound() {
			assertThatThrownBy(() -> queueApplicationService.getPosition(999L))
				.isInstanceOf(CoreException.class)
				.satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("대기 중인 사용자는 순번과 예상 대기 시간을 반환하고, 토큰은 아직 없다.")
		@Test
		void waiting_returnsPositionWithoutToken() {
			queueApplicationService.enter(1L);
			queueApplicationService.enter(2L);

			QueuePositionResult result = queueApplicationService.getPosition(2L);

			assertThat(result.position()).isEqualTo(2L);
			assertThat(result.estimatedWaitSeconds()).isEqualTo(600L);
			assertThat(result.admissionToken()).isNull();
		}

		@DisplayName("스케줄러가 입장시켜 토큰이 발급되면 순번 0, 예상 대기 0, 토큰이 포함된다.")
		@Test
		void admitted_returnsToken() {
			queueApplicationService.enter(1L);

			// 스케줄러를 직접 호출해 입장(토큰 발급) 시뮬레이션
			queueAdmissionScheduler.admit();

			QueuePositionResult result = queueApplicationService.getPosition(1L);

			assertThat(result.position()).isZero();
			assertThat(result.estimatedWaitSeconds()).isZero();
			assertThat(result.admissionToken()).isNotBlank();
		}
	}
}
