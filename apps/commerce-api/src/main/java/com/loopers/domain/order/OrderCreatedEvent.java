package com.loopers.domain.order;

public record OrderCreatedEvent(
	Long orderId,
	Long userId
) {
}
