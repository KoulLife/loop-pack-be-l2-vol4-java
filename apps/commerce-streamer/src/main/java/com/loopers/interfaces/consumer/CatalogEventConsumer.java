package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.infrastructure.event.EventHandledEntity;
import com.loopers.infrastructure.event.EventHandledJpaRepository;
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository;
import com.loopers.interfaces.consumer.message.CatalogEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class CatalogEventConsumer {

    private final ObjectMapper objectMapper;
    private final EventHandledJpaRepository eventHandledRepository;
    private final ProductMetricsJpaRepository productMetricsRepository;

    @KafkaListener(topics = "catalog-events", containerFactory = KafkaConfig.BATCH_LISTENER)
    @Transactional
    public void consume(List<ConsumerRecord<Object, Object>> records, Acknowledgment ack) {
        for (ConsumerRecord<Object, Object> record : records) {
            try {
                CatalogEventMessage message = objectMapper.readValue((byte[]) record.value(), CatalogEventMessage.class);
                if (eventHandledRepository.existsById(message.eventId())) {
                    log.debug("[CatalogConsumer] skipped duplicate eventId={}", message.eventId());
                    continue;
                }
                processEvent(message);
                eventHandledRepository.save(EventHandledEntity.of(message.eventId()));
            } catch (Exception e) {
                log.error("[CatalogConsumer] failed to process record offset={}", record.offset(), e);
            }
        }
        ack.acknowledge();
    }

    private void processEvent(CatalogEventMessage message) {
        if ("PRODUCT_VIEWED".equals(message.type())) {
            productMetricsRepository.upsert(message.productId(), 1L, 0L, 0L);
            log.info("[CatalogConsumer] view_count+1 productId={}", message.productId());
        }
    }
}
