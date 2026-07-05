package com.loopers.domain.coupon;

public record CouponIssueRequestedEvent(
	String eventId,
	Long couponId,
	Long userId
) {
}
