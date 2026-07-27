package com.algotradex.repository;

import com.algotradex.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByUserId(UUID userId);
    java.util.Optional<Position> findByStrategyIdAndInstrument_IdAndStatus(UUID strategyId, UUID instrumentId, com.algotradex.model.enums.PositionStatus status);
}
