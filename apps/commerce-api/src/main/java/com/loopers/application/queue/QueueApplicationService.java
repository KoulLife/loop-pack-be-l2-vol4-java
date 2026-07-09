package com.loopers.application.queue;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.loopers.domain.queue.QueueRepository;
import com.loopers.support.config.QueueProperties;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class QueueApplicationService {

	private final QueueRepository queueRepository;
	private final QueueProperties queueProperties;

	public QueueEnterResult enter(Long userId) {
		// 이미 입장 토큰을 발급받은 사용자는 재진입(재대기)시키지 않는다.
		if (queueRepository.getAdmissionToken(userId).isPresent()) {
			return new QueueEnterResult(userId, false, 0L, queueRepository.size(), 0L);
		}

		boolean entered = queueRepository.enqueue(userId);

		Long rank = queueRepository.rank(userId);
		if (rank == null) {
			throw new CoreException(ErrorType.INTERNAL_ERROR, "대기열 진입 후 순번 조회에 실패했습니다.");
		}

		long position = rank + 1;
		long totalWaitingCount = queueRepository.size();

		return new QueueEnterResult(
			userId,
			entered,
			position,
			totalWaitingCount,
			estimateWaitSeconds(position)
		);
	}

	public QueuePositionResult getPosition(Long userId) {
		Optional<String> admissionToken = queueRepository.getAdmissionToken(userId);
		if (admissionToken.isPresent()) {
			// 입장 토큰이 발급된 상태 = 순번 0, 예상 대기 시간 0.
			return new QueuePositionResult(userId, 0L, queueRepository.size(), 0L, admissionToken.get());
		}

		Long rank = queueRepository.rank(userId);
		if (rank == null) {
			throw new CoreException(ErrorType.NOT_FOUND, "대기열에 진입하지 않은 사용자입니다.");
		}

		long position = rank + 1;
		long totalWaitingCount = queueRepository.size();

		return new QueuePositionResult(
			userId,
			position,
			totalWaitingCount,
			estimateWaitSeconds(position),
			null
		);
	}

	/**
	 * 예상 대기 시간(초) = 앞으로 필요한 스케줄 실행 횟수 × 실행 주기.
	 * 실행 횟수 = ceil(순번 / 배치 크기).
	 */
	private long estimateWaitSeconds(long position) {
		int batchSize = Math.max(queueProperties.admission().batchSize(), 1);
		long intervalMs = queueProperties.admission().intervalMs();
		long ticks = (long) Math.ceil((double) position / batchSize);
		return (long) Math.ceil(ticks * intervalMs / 1000.0);
	}
}
