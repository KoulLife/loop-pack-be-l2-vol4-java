package com.loopers.application.queue;

public record QueueEnterResult(
	Long userId,
	boolean entered,
	Long position,
	Long totalWaitingCount,
	Long estimatedWaitSeconds
) {
}
