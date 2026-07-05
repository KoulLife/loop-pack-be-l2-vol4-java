package com.loopers.interfaces.consumer.message;

import java.time.ZonedDateTime;

public record CouponIssueRequestMessage(
    String eventId,
    String type,
    Long couponId,
    Long userId,
    ZonedDateTime occurredAt
) {}
