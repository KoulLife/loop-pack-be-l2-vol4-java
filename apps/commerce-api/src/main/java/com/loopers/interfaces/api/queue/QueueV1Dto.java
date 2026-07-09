package com.loopers.interfaces.api.queue;

import com.loopers.application.queue.QueueEnterResult;
import com.loopers.application.queue.QueuePositionResult;

public class QueueV1Dto {

	public record EnterResponse(
		Long userId,
		boolean entered,
		Long position,
		Long totalWaitingCount,
		Long estimatedWaitSeconds
	) {
		public static EnterResponse from(QueueEnterResult result) {
			return new EnterResponse(
				result.userId(),
				result.entered(),
				result.position(),
				result.totalWaitingCount(),
				result.estimatedWaitSeconds()
			);
		}
	}

	public record PositionResponse(
		Long userId,
		Long position,
		Long totalWaitingCount,
		Long estimatedWaitSeconds,
		String admissionToken
	) {
		public static PositionResponse from(QueuePositionResult result) {
			return new PositionResponse(
				result.userId(),
				result.position(),
				result.totalWaitingCount(),
				result.estimatedWaitSeconds(),
				result.admissionToken()
			);
		}
	}
}
