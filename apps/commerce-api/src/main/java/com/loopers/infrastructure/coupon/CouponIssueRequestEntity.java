package com.loopers.infrastructure.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.coupon.IssueRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupon_issue_requests")
public class CouponIssueRequestEntity extends BaseEntity {

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IssueRequestStatus status;

    private CouponIssueRequestEntity(String requestId, Long userId, Long couponId, IssueRequestStatus status) {
        this.requestId = requestId;
        this.userId = userId;
        this.couponId = couponId;
        this.status = status;
    }

    public static CouponIssueRequestEntity pending(String requestId, Long userId, Long couponId) {
        return new CouponIssueRequestEntity(requestId, userId, couponId, IssueRequestStatus.PENDING);
    }
}
