package com.loopers.application.queue;

public record QueuePositionResult(
	Long userId,
	Long position,
	Long totalWaitingCount,
	Long estimatedWaitSeconds,
	String admissionToken
) {
}
