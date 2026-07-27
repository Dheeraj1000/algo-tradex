package com.algotradex.repository;

import com.algotradex.model.StrategyState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StrategyStateRepository extends JpaRepository<StrategyState, UUID> {
    java.util.Optional<StrategyState> findByStrategyId(UUID strategyId);
}
