package com.loopers.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 대기열(입장 스케줄러 · 입장 토큰) 관련 설정.
 * 배치 크기 산정 근거는 docs/round8-queue.md 참고.
 */
@ConfigurationProperties("queue")
public record QueueProperties(
	Admission admission,
	Token token
) {

	public QueueProperties {
		if (admission == null) {
			admission = new Admission(100, 1000L);
		}
		if (token == null) {
			token = new Token(300L);
		}
	}

	/**
	 * @param batchSize  한 번의 스케줄 실행에서 입장시킬 인원 수
	 * @param intervalMs 스케줄러 실행 주기 (ms)
	 */
	public record Admission(int batchSize, long intervalMs) {
	}

	/**
	 * @param ttlSeconds 입장 토큰의 유효 시간 (초)
	 */
	public record Token(long ttlSeconds) {
	}
}
