package com.loopers.infrastructure.kafka.message;

import java.time.ZonedDateTime;

public record CatalogEventMessage(
    String eventId,
    String type,
    Long productId,
    ZonedDateTime occurredAt
) {}
