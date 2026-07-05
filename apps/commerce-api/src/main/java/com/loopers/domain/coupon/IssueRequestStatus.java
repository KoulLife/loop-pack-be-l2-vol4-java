package com.loopers.domain.coupon;

/**
 * 비동기 쿠폰 발급 요청의 처리 상태.
 * PENDING(접수) → ISSUED(발급 완료) / SOLD_OUT(수량 소진) / DUPLICATED(중복 발급)
 */
public enum IssueRequestStatus {
    PENDING, ISSUED, SOLD_OUT, DUPLICATED
}
