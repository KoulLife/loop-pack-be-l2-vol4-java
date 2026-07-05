package com.loopers.infrastructure.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponStockJpaRepository extends JpaRepository<CouponStockEntity, Long> {

    /**
     * 남은 수량이 있을 때만 발급 수량을 1 증가시킨다. 원자적 조건부 UPDATE 로 선착순 경합을 제어한다.
     *
     * @return 갱신된 행 수. 1이면 발급 성공, 0이면 수량 소진(SOLD_OUT).
     */
    @Modifying
    @Query("UPDATE CouponStockEntity s SET s.issuedCount = s.issuedCount + 1 "
        + "WHERE s.couponId = :couponId AND s.issuedCount < s.quantity")
    int tryIssue(@Param("couponId") Long couponId);
}
