package com.loopers.application.coupon;

import com.loopers.domain.coupon.IssueRequestStatus;
import com.loopers.infrastructure.coupon.CouponIssueRequestEntity;

public record IssueRequestInfo(
    String requestId,
    Long couponId,
    Long userId,
    IssueRequestStatus status
) {
    public static IssueRequestInfo from(CouponIssueRequestEntity entity) {
        return new IssueRequestInfo(
            entity.getRequestId(),
            entity.getCouponId(),
            entity.getUserId(),
            entity.getStatus()
        );
    }
}
