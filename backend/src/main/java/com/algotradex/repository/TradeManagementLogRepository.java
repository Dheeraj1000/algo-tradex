package com.algotradex.repository;

import com.algotradex.model.TradeManagementLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeManagementLogRepository extends JpaRepository<TradeManagementLog, UUID> {
    List<TradeManagementLog> findByTradeSignalIdOrderByTimestampDesc(UUID tradeSignalId);
}
