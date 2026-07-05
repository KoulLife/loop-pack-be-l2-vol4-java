package com.loopers.infrastructure.coupon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 선착순 쿠폰의 한정 수량 재고. couponId 단위로 발급 수량을 원자적으로 제어한다.
 */
@Getter
@Entity
@Table(name = "coupon_stock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponStockEntity {

    @Id
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount;

    private CouponStockEntity(Long couponId, int quantity) {
        this.couponId = couponId;
        this.quantity = quantity;
        this.issuedCount = 0;
    }

    public static CouponStockEntity of(Long couponId, int quantity) {
        return new CouponStockEntity(couponId, quantity);
    }
}
