package com.loopers.infrastructure.coupon;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * user_coupons 테이블에 발급 결과를 기록하기 위한 streamer 측 매핑. commerce-api 의 UserCouponEntity 와 동일 테이블/컬럼을 공유한다.
 */
@Getter
@Entity
@Table(name = "user_coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedUserCouponEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status;

    @Version
    private Long version;

    private IssuedUserCouponEntity(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = CouponStatus.AVAILABLE;
    }

    public static IssuedUserCouponEntity issue(Long userId, Long couponId) {
        return new IssuedUserCouponEntity(userId, couponId);
    }
}
