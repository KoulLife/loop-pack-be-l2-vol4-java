package com.loopers.domain.payment;

public record PaymentFailedEvent(
	Long orderId,
	Long userId,
	String reason
) {
}
