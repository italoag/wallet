package dev.bloco.wallet.hub.infra.provider.data.repository;

import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateMachineRepository<J, K> extends JpaStateMachineRepository {
}
