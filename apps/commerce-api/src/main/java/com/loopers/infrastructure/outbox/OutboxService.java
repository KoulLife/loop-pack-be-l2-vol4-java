package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.order.OrderModel;
import com.loopers.infrastructure.kafka.message.CouponIssueRequestMessage;
import com.loopers.infrastructure.kafka.message.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxService {

    public static final String ORDER_EVENTS_TOPIC = "order-events";
	public static final String COUPON_ISSUE_REQUESTS_TOPIC = "coupon-issue-requests";

	private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;

    public void saveOrderCreated(OrderModel order, Map<Long, Integer> quantities) {
        String eventId = UUID.randomUUID().toString();
        List<OrderEventMessage.Item> items = quantities.entrySet().stream()
            .map(e -> new OrderEventMessage.Item(e.getKey(), e.getValue()))
            .toList();
        OrderEventMessage message = new OrderEventMessage(eventId, "ORDER_CREATED", order.getId(), order.getUserId(), items, ZonedDateTime.now());

        try {
            String payload = objectMapper.writeValueAsString(message);
            outboxJpaRepository.save(OutboxEntity.create(eventId, ORDER_EVENTS_TOPIC, String.valueOf(order.getId()), payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderEventMessage for orderId={}", order.getId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSentByOrderId(Long orderId) {
        outboxJpaRepository.findTopByPartitionKeyAndTopicAndStatus(
            String.valueOf(orderId), ORDER_EVENTS_TOPIC, OutboxStatus.PENDING
        ).ifPresent(outbox -> {
            outbox.markSent();
            outboxJpaRepository.save(outbox);
        });
    }

	public String saveCouponIssueRequested(Long couponId, Long userId) {
		String eventId = UUID.randomUUID().toString();
		CouponIssueRequestMessage message = new CouponIssueRequestMessage(
			eventId, "COUPON_ISSUE_REQUESTED", couponId, userId, ZonedDateTime.now()
		);
		try {
			String payload = objectMapper.writeValueAsString(message);
			outboxJpaRepository.save(OutboxEntity.create(eventId, COUPON_ISSUE_REQUESTS_TOPIC, String.valueOf(couponId), payload));
		} catch (JsonProcessingException e) {
			log.error("couponId={} 에 대한 CouponIssueRequestMessage 직렬화에 실패했습니다.", couponId, e);
			return null;
		}
		return eventId;
	}
}
