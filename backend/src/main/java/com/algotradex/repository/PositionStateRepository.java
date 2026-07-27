package com.algotradex.repository;

import com.algotradex.model.PositionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionStateRepository extends JpaRepository<PositionState, UUID> {
    Optional<PositionState> findByTradeSignalId(UUID tradeSignalId);
}
