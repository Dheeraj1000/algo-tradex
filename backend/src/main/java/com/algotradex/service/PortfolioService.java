package com.algotradex.service;

import com.algotradex.broker.BrokerAdapter;
import com.algotradex.model.BrokerAccount;
import com.algotradex.model.Position;
import com.algotradex.repository.PositionRepository;
import com.algotradex.model.enums.BrokerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final BrokerService brokerService;

    public List<Position> getPositions(UUID userId) {
        List<BrokerAccount> accounts = brokerService.getUserBrokerAccounts(userId);
        // 1. Get all local positions (Paper Trading & History)
        List<Position> finalPositions = new ArrayList<>(positionRepository.findByUserId(userId));
        
        // 2. Fetch Live positions from connected brokers (e.g. Dhan)
        for (BrokerAccount account : accounts) {
            if (BrokerStatus.CONNECTED == account.getStatus()) {
                try {
                    List<Position> brokerPositions = syncPositionsWithBroker(account);
                    if (brokerPositions != null) {
                        // In a full implementation, we'd merge/reconcile these carefully.
                        // For now, just add the live ones to the dashboard view.
                        finalPositions.addAll(brokerPositions);
                    }
                } catch (Exception e) {
                    // Ignore error for a single broker to prevent breaking the portfolio page
                }
            }
        }
        
        for (Position pos : finalPositions) {
            if (pos.getStatus() == com.algotradex.model.enums.PositionStatus.OPEN && pos.getCurrentPrice() != null) {
                java.math.BigDecimal lotSize = java.math.BigDecimal.ONE;
                String symbol = pos.getInstrument().getTradingSymbol();
                if (symbol.startsWith("^NSEI")) lotSize = java.math.BigDecimal.valueOf(50);
                else if (symbol.startsWith("^BSESN")) lotSize = java.math.BigDecimal.valueOf(10);
                
                java.math.BigDecimal diff;
                if (pos.getSide() == com.algotradex.model.enums.OrderSide.BUY) {
                    diff = pos.getCurrentPrice().subtract(pos.getAvgEntryPrice());
                } else {
                    diff = pos.getAvgEntryPrice().subtract(pos.getCurrentPrice());
                }
                pos.setUnrealizedPnl(diff.multiply(java.math.BigDecimal.valueOf(pos.getQuantity())).multiply(lotSize));
            }
        }
        return finalPositions;
    }
    
    public List<Position> syncPositionsWithBroker(BrokerAccount account) {
        BrokerAdapter adapter = brokerService.getAdapterForAccount(account);
        if (!adapter.isConnected()) {
            brokerService.connectBroker(account.getId());
        }
        
        List<Position> brokerPositions = adapter.getPositions(account);
        // Note: Full implementation would reconcile these with local DB.
        // For now, we'll just return the mock positions.
        return brokerPositions;
    }
}
