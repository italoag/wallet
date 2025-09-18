package dev.bloco.wallet.hub.infra.provider.data.repository;

import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;

/**
 * Spring Data repository for Spring Statemachine JPA persistence.
 *<p/>
 * Extends {@link JpaStateMachineRepository} so Spring Data can generate an implementation
 * and expose it as a bean for autowiring in configuration.
 */
public interface StateMachineRepository extends JpaStateMachineRepository {
}
