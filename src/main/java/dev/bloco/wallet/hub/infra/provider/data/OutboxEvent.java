package dev.bloco.wallet.hub.infra.provider.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents an event in the outbox table used for implementing an eventual
 * consistency pattern. This entity serves as a record of events that need to be
 * processed and potentially sent to external systems.
 *<p/>
 * An {@code OutboxEvent} stores the metadata and payload of the event, along with
 * its state indicating whether it has been sent or not. The event is marked as sent
 * when the corresponding processing completes successfully.
 *<p/>
 * Key Features:
 * - Each event is uniquely identified by an {@code id} that is auto-generated.
 * - The {@code eventType} property indicates the type or category of the event.
 * - The {@code payload} property contains the data associated with the event.
 * - The {@code correlationId} property optionally relates this event to other records
 *   or workflows within the system.
 * - The {@code createdAt} property captures the timestamp when the event was created.
 * - The {@code sent} property tracks whether the event has been processed successfully.
 *<p/>
 * This class leverages JPA annotations for persistence and relies on Hibernate's
 * capabilities for equality comparisons and hashing to ensure proper handling of instances.
 *<p/>
 * Design Considerations:
 * - The {@code equals()} and {@code hashCode()} methods are overridden to ensure
 *   consistent entity comparisons, particularly when dealing with proxy instances.
 * - The initial creation timestamp and the initial sent state are defined as defaults.
 *<p/>
 * The {@link OutboxService} and {@link OutboxWorker} collaborate to manage the lifecycle
 * of {@code OutboxEvent} instances. This includes:
 * - Saving new events to the database.
 * - Retrieving unsent events for processing.
 * - Marking events as sent upon successful processing.
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table(name = "outbox", indexes = {@Index(name = "idx_outbox_created_at", columnList = "created_at")})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent", nullable = false)
    private boolean sent = false;

    // Getters and Setters

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hp ? hp.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hp ? hp.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        OutboxEvent that = (OutboxEvent) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hp ? hp.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

