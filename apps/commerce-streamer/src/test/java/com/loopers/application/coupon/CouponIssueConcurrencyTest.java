package com.loopers.application.coupon;

import com.loopers.infrastructure.coupon.CouponStockEntity;
import com.loopers.infrastructure.coupon.CouponStockJpaRepository;
import com.loopers.infrastructure.coupon.IssuedUserCouponJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("concurrency")
@SpringBootTest
class CouponIssueConcurrencyTest {

    @Autowired private CouponIssueService couponIssueService;
    @Autowired private CouponStockJpaRepository couponStockRepository;
    @Autowired private IssuedUserCouponJpaRepository userCouponRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("100장 한정 쿠폰에 300명이 동시에 발급을 요청해도 정확히 100장만 발급되고 초과 발급은 0건이다.")
    @Test
    void issuesExactlyLimitedQuantity_underConcurrentRequests() throws InterruptedException {
        // arrange
        int quantity = 100;
        int threads = 300;
        long couponId = 1L;
        couponStockRepository.save(CouponStockEntity.of(couponId, quantity));

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(32);

        // act: 서로 다른 userId/requestId 로 동시에 발급 요청
        for (int i = 0; i < threads; i++) {
            final long userId = i + 1L;
            final String requestId = UUID.randomUUID().toString();
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    couponIssueService.issue(requestId, couponId, userId);
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // assert: 정확히 100장만 발급 + 초과 발급 0
        long issuedUserCoupons = userCouponRepository.countByCouponId(couponId);
        int issuedCount = couponStockRepository.findById(couponId).orElseThrow().getIssuedCount();

        assertThat(issuedUserCoupons).isEqualTo(quantity);
        assertThat(issuedCount).isEqualTo(quantity);
    }
}
