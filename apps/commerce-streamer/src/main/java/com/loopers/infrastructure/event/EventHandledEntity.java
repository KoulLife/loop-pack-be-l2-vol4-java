package com.loopers.infrastructure.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Getter
@Entity
@Table(name = "event_handled")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandledEntity {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "handled_at", nullable = false)
    private ZonedDateTime handledAt;

    @PrePersist
    private void prePersist() {
        this.handledAt = ZonedDateTime.now();
    }

    public static EventHandledEntity of(String eventId) {
        EventHandledEntity entity = new EventHandledEntity();
        entity.eventId = eventId;
        return entity;
    }
}
