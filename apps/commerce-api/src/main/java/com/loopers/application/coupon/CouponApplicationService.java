package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueRequestedEvent;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCouponModel;
import com.loopers.domain.coupon.UserCouponRepository;

import java.time.ZonedDateTime;

import com.loopers.infrastructure.coupon.CouponIssueRequestEntity;
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository;
import com.loopers.infrastructure.outbox.OutboxService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CouponApplicationService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
	private final OutboxService outboxService;
	private final ApplicationEventPublisher eventPublisher;
	private final CouponIssueRequestJpaRepository couponIssueRequestRepository;

	@Transactional
    public CouponInfo issueCoupon(Long userId, Long couponId) {
        CouponModel coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
        if (ZonedDateTime.now().isAfter(coupon.getExpiredAt())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.");
        }
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 발급된 쿠폰입니다.");
        }
        UserCouponModel userCoupon = userCouponRepository.save(new UserCouponModel(userId, couponId));
        return CouponInfo.from(userCoupon, coupon);
    }

    @Transactional(readOnly = true)
    public List<CouponInfo> getUserCoupons(Long userId) {
        return userCouponRepository.findAllByUserId(userId).stream()
            .map(userCoupon -> {
                CouponModel coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + userCoupon.getCouponId() + "] 쿠폰을 찾을 수 없습니다."));
                return CouponInfo.from(userCoupon, coupon);
            })
            .toList();
    }

    @Transactional
    public CouponAdminInfo createCoupon(String name, CouponType type, int value, int minOrderAmount, ZonedDateTime expiredAt) {
        CouponModel coupon = couponRepository.save(new CouponModel(name, type, value, minOrderAmount, expiredAt));
        return CouponAdminInfo.from(coupon);
    }

    @Transactional(readOnly = true)
    public Page<CouponAdminInfo> getCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable).map(CouponAdminInfo::from);
    }

    @Transactional(readOnly = true)
    public CouponAdminInfo getCoupon(Long couponId) {
        CouponModel coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
        return CouponAdminInfo.from(coupon);
    }

    @Transactional
    public CouponAdminInfo updateCoupon(Long couponId, String name, CouponType type, int value, int minOrderAmount, ZonedDateTime expiredAt) {
        CouponModel coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
        coupon.update(name, type, value, minOrderAmount, expiredAt);
        return CouponAdminInfo.from(couponRepository.save(coupon));
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
        couponRepository.delete(couponId);
    }

    @Transactional(readOnly = true)
    public Page<CouponIssueAdminInfo> getCouponIssues(Long couponId, Pageable pageable) {
        couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
        return userCouponRepository.findAllByCouponId(couponId, pageable).map(CouponIssueAdminInfo::from);
    }

	/**
	 * 비동기 쿠폰 발급 요청을 접수한다. 실제 발급(수량 제어/중복 방지)은 Consumer가 수행한다.
	 * 접수 성공 시 발급 결과 조회에 사용할 requestId를 반환한다.
	 */
	@Transactional
	public String requestIssue(Long userId, Long couponId) {
		CouponModel coupon = couponRepository.findById(couponId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
		if (ZonedDateTime.now().isAfter(coupon.getExpiredAt())) {
			throw new CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.");
		}
		if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
			throw new CoreException(ErrorType.CONFLICT, "이미 발급된 쿠폰입니다.");
		}
		String requestId = outboxService.saveCouponIssueRequested(couponId, userId);
		couponIssueRequestRepository.save(CouponIssueRequestEntity.pending(requestId, userId, couponId));
		eventPublisher.publishEvent(new CouponIssueRequestedEvent(requestId, couponId, userId));
		return requestId;
	}

	@Transactional(readOnly = true)
	public IssueRequestInfo getIssueRequestStatus(String requestId) {
		return couponIssueRequestRepository.findByRequestId(requestId)
			.map(IssueRequestInfo::from)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[requestId = " + requestId + "] 발급 요청을 찾을 수 없습니다."));
	}
}
