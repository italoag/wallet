package dev.bloco.wallet.hub.domain.model.common;

import dev.bloco.wallet.hub.domain.event.common.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class AggregateRoot extends Entity {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot(UUID id) {
        super(id);
    }

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearEvents() {
        domainEvents.clear();
    }
}