package com.loopers.infrastructure.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedUserCouponJpaRepository extends JpaRepository<IssuedUserCouponEntity, Long> {
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
    long countByCouponId(Long couponId);
}
