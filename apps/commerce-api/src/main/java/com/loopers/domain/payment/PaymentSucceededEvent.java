package com.loopers.domain.payment;

public record PaymentSucceededEvent(
	Long orderId,
	Long userId,
	Long amount
) {
}
