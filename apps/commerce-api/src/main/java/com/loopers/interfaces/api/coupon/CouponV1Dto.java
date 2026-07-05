package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.coupon.IssueRequestInfo;
import com.loopers.domain.coupon.CouponStatus;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.IssueRequestStatus;

import java.time.ZonedDateTime;
import java.util.List;

public class CouponV1Dto {

    public enum CouponTypeDto {
        FIXED, RATE;

        public static CouponTypeDto from(CouponType type) {
            return CouponTypeDto.valueOf(type.name());
        }
    }

    public enum CouponStatusDto {
        AVAILABLE, USED, EXPIRED;

        public static CouponStatusDto from(CouponStatus status) {
            return CouponStatusDto.valueOf(status.name());
        }
    }

    public record CouponResponse(
        Long userCouponId,
        Long couponId,
        String name,
        CouponTypeDto type,
        int value,
        int minOrderAmount,
        ZonedDateTime expiredAt,
        CouponStatusDto status
    ) {
        public static CouponResponse from(CouponInfo info) {
            return new CouponResponse(
                info.userCouponId(),
                info.couponId(),
                info.name(),
                CouponTypeDto.from(info.type()),
                info.value(),
                info.minOrderAmount(),
                info.expiredAt(),
                CouponStatusDto.from(info.status())
            );
        }
    }

    public record CouponListResponse(List<CouponResponse> coupons) {
        public static CouponListResponse from(List<CouponInfo> infos) {
            return new CouponListResponse(infos.stream().map(CouponResponse::from).toList());
        }
    }

    public record IssueRequestResponse(
        String requestId,
        Long couponId,
        IssueRequestStatus status
    ) {
        public static IssueRequestResponse accepted(String requestId, Long couponId) {
            return new IssueRequestResponse(requestId, couponId, IssueRequestStatus.PENDING);
        }
    }

    public record IssueRequestStatusResponse(
        String requestId,
        Long couponId,
        Long userId,
        IssueRequestStatus status
    ) {
        public static IssueRequestStatusResponse from(IssueRequestInfo info) {
            return new IssueRequestStatusResponse(info.requestId(), info.couponId(), info.userId(), info.status());
        }
    }

    public record IssueResponse(
        Long userCouponId,
        Long couponId,
        String name,
        CouponTypeDto type,
        int value,
        int minOrderAmount,
        ZonedDateTime expiredAt,
        CouponStatusDto status
    ) {
        public static IssueResponse from(CouponInfo info) {
            return new IssueResponse(
                info.userCouponId(),
                info.couponId(),
                info.name(),
                CouponTypeDto.from(info.type()),
                info.value(),
                info.minOrderAmount(),
                info.expiredAt(),
                CouponStatusDto.from(info.status())
            );
        }
    }
}
