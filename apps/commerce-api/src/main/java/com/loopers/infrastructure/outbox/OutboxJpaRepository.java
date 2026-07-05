package com.loopers.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, Long> {

    Optional<OutboxEntity> findTopByPartitionKeyAndTopicAndStatus(String partitionKey, String topic, OutboxStatus status);

    List<OutboxEntity> findByStatusAndCreatedAtBefore(OutboxStatus status, ZonedDateTime before);

	Optional<OutboxEntity> findByEventId(String eventId);
}
