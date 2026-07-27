package com.algotradex.engine;

import com.algotradex.client.AiServiceClient;
import com.algotradex.model.PositionState;
import com.algotradex.model.TradeManagementLog;
import com.algotradex.model.TradeSignal;
import com.algotradex.model.enums.OrderStatus;
import com.algotradex.repository.PositionStateRepository;
import com.algotradex.repository.TradeManagementLogRepository;
import com.algotradex.repository.TradeSignalRepository;
import com.algotradex.service.BrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeManagementMonitor {

    private final TradeSignalRepository tradeSignalRepository;
    private final PositionStateRepository positionStateRepository;
    private final TradeManagementLogRepository logRepository;
    private final AiServiceClient aiServiceClient;
    private final RiskEngineService riskEngineService;
    private final TrailingStopEngine trailingStopEngine;
    private final BrokerService brokerService;
    private final com.algotradex.service.HistoricalDataService historicalDataService;

    @Scheduled(fixedDelay = 5000)
    public void monitorActivePositions() {
        // 1. Fetch all OPEN signals (open positions)
        // Note: For a real app, you'd fetch open positions from the broker or DB.
        // Assuming TradeSignal with OPEN means it's open. 
        // A new status might be needed like 'CLOSED' to differentiate.
        List<TradeSignal> openTrades = tradeSignalRepository.findAll(); // Should filter by status

        for (TradeSignal trade : openTrades) {
            if (trade.getExecutionStatus() != OrderStatus.OPEN) continue;
            
            // Fetch live price from broker
            double currentPrice = fetchLivePrice(trade);
            
            Optional<PositionState> positionOpt = positionStateRepository.findByTradeSignalId(trade.getId());
            PositionState position = positionOpt.orElseGet(() -> {
                PositionState ps = PositionState.builder()
                        .tradeSignalId(trade.getId())
                        .currentQuantity(1) // stub
                        .isInRecovery(false)
                        .build();
                return positionStateRepository.save(ps);
            });

            // Prepare AI Request
            AiServiceClient.ManageTradeRequest req = new AiServiceClient.ManageTradeRequest();
            req.setSymbol(trade.getTargetSymbol()); // Simplification
            req.setTarget_option(trade.getTargetSymbol());
            req.setSignal_type(trade.getSignalType().name());
            req.setEntry_price(trade.getEntryPrice() != null ? trade.getEntryPrice() : currentPrice);
            req.setCurrent_price(currentPrice);
            req.setStop_loss(trade.getCurrentStopLoss() != null ? trade.getCurrentStopLoss() : currentPrice * 0.95);
            req.setTarget(trade.getEntryPrice() != null ? trade.getEntryPrice() * 1.05 : currentPrice * 1.05); // Stub 5% target
            req.setQuantity(position.getCurrentQuantity());
            
            boolean slBreached = currentPrice <= req.getStop_loss();
            req.setStop_loss_breached(slBreached);

            // Call AI
            Optional<AiServiceClient.ManageTradeResponse> aiResponseOpt = aiServiceClient.manageTrade(req);
            
            if (aiResponseOpt.isPresent()) {
                AiServiceClient.ManageTradeResponse aiResponse = aiResponseOpt.get();
                
                // Log Action
                TradeManagementLog tml = TradeManagementLog.builder()
                        .tradeSignalId(trade.getId())
                        .actionTaken(aiResponse.getAction())
                        .recoveryProbability(aiResponse.getRecovery_probability())
                        .explanation(aiResponse.getExplanation())
                        .optionPrice(currentPrice)
                        .build();
                logRepository.save(tml);
                
                // Execute Action
                executeAiAction(trade, position, aiResponse, currentPrice);
            }
        }
    }
    
    private void executeAiAction(TradeSignal trade, PositionState position, AiServiceClient.ManageTradeResponse aiResponse, double currentPrice) {
        String action = aiResponse.getAction();
        log.info("Trade {} AI Action: {} - {}", trade.getId(), action, aiResponse.getExplanation());
        
        switch (action) {
            case "EXIT":
                // brokerService.placeOrder(EXIT);
                trade.setExecutionStatus(OrderStatus.COMPLETE);
                trade.setExitReason(aiResponse.getExplanation());
                trade.setExitPrice(currentPrice);
                tradeSignalRepository.save(trade);
                break;
            case "MOVE_STOP_LOSS":
                Double newStop = trailingStopEngine.calculateTrailingStop(trade, currentPrice);
                trade.setCurrentStopLoss(newStop);
                trade.setAiExplanation(aiResponse.getExplanation());
                tradeSignalRepository.save(trade);
                break;
            case "PARTIAL_EXIT":
                position.setCurrentQuantity(position.getCurrentQuantity() / 2);
                int currentExits = position.getPartialExitsCount() == null ? 0 : position.getPartialExitsCount();
                position.setPartialExitsCount(currentExits + 1);
                positionStateRepository.save(position);
                break;
            case "HOLD":
            default:
                // Do nothing
                break;
        }
    }
    
    private double fetchLivePrice(TradeSignal trade) {
        String symbol = trade.getTargetSymbol();
        if (symbol != null && symbol.endsWith("USDT")) {
            java.math.BigDecimal binanceLtp = historicalDataService.getBinanceLtp(symbol);
            if (binanceLtp != null) {
                return binanceLtp.doubleValue();
            }
        }
        // Fallback for Indian Market: Use entry price so we don't trigger artificial stop losses
        return trade.getEntryPrice() != null ? trade.getEntryPrice() : 100.0;
    }
}
