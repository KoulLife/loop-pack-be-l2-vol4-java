package com.loopers.infrastructure.coupon;

/**
 * 비동기 쿠폰 발급 요청의 처리 상태. commerce-api 와 동일한 이름을 공유한다.
 */
public enum IssueRequestStatus {
    PENDING, ISSUED, SOLD_OUT, DUPLICATED
}
