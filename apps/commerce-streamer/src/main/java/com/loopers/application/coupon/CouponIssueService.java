package com.loopers.application.coupon;

import com.loopers.infrastructure.coupon.CouponIssueRequestEntity;
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository;
import com.loopers.infrastructure.coupon.CouponStockJpaRepository;
import com.loopers.infrastructure.coupon.IssueRequestStatus;
import com.loopers.infrastructure.coupon.IssuedUserCouponEntity;
import com.loopers.infrastructure.coupon.IssuedUserCouponJpaRepository;
import com.loopers.infrastructure.event.EventHandledEntity;
import com.loopers.infrastructure.event.EventHandledJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선착순 쿠폰 발급의 실제 처리 단위.
 * 멱등(event_handled) · 중복 방지(DUPLICATED) · 원자적 수량 제어(SOLD_OUT) · 발급(ISSUED)을 단일 트랜잭션으로 수행한다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CouponIssueService {

    private final EventHandledJpaRepository eventHandledRepository;
    private final CouponStockJpaRepository couponStockRepository;
    private final IssuedUserCouponJpaRepository userCouponRepository;
    private final CouponIssueRequestJpaRepository issueRequestRepository;

    @Transactional
    public void issue(String requestId, Long couponId, Long userId) {
        if (eventHandledRepository.existsById(requestId)) {
            log.debug("[CouponIssue] skipped duplicate requestId={}", requestId);
            return;
        }

        IssueRequestStatus result = process(requestId, couponId, userId);
        updateRequestStatus(requestId, result);
        eventHandledRepository.save(EventHandledEntity.of(requestId));
        log.info("[CouponIssue] requestId={} couponId={} userId={} → {}", requestId, couponId, userId, result);
    }

    private IssueRequestStatus process(String requestId, Long couponId, Long userId) {
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            return IssueRequestStatus.DUPLICATED;
        }
        // 남은 수량이 있을 때만 원자적으로 발급 카운트 증가. 영향 행이 0이면 소진.
        if (couponStockRepository.tryIssue(couponId) == 0) {
            return IssueRequestStatus.SOLD_OUT;
        }
        userCouponRepository.save(IssuedUserCouponEntity.issue(userId, couponId));
        return IssueRequestStatus.ISSUED;
    }

    private void updateRequestStatus(String requestId, IssueRequestStatus status) {
        issueRequestRepository.findByRequestId(requestId)
            .ifPresentOrElse(
                request -> request.markStatus(status),
                () -> log.warn("[CouponIssue] issue request row not found requestId={}", requestId)
            );
    }
}
