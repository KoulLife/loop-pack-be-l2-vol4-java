package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.coupon.CouponIssueService;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.interfaces.consumer.message.CouponIssueRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 선착순 쿠폰 발급 요청(coupon-issue-requests) Consumer.
 * key=couponId 파티셔닝으로 동일 쿠폰 요청이 단일 파티션에 순차 적재되어 경합이 완화되며,
 * 실제 수량 제어/중복 방지/멱등은 {@link CouponIssueService} 트랜잭션에서 보장한다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CouponIssueConsumer {

    private final ObjectMapper objectMapper;
    private final CouponIssueService couponIssueService;

    @KafkaListener(topics = "coupon-issue-requests", containerFactory = KafkaConfig.BATCH_LISTENER)
    public void consume(List<ConsumerRecord<Object, Object>> records, Acknowledgment ack) {
        for (ConsumerRecord<Object, Object> record : records) {
            try {
                CouponIssueRequestMessage message = objectMapper.readValue((byte[]) record.value(), CouponIssueRequestMessage.class);
                couponIssueService.issue(message.eventId(), message.couponId(), message.userId());
            } catch (Exception e) {
                log.error("[CouponIssueConsumer] failed to process record offset={}", record.offset(), e);
            }
        }
        ack.acknowledge();
    }
}
