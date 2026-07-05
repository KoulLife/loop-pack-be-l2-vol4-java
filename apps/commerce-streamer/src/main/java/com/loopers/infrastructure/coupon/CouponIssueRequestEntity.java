package com.loopers.infrastructure.coupon;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * coupon_issue_requests 테이블의 streamer 측 매핑. Consumer 가 처리 결과 상태를 갱신한다.
 */
@Getter
@Entity
@Table(name = "coupon_issue_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public void markStatus(IssueRequestStatus status) {
        this.status = status;
    }
}
