package com.algotradex.engine;

import com.algotradex.model.Strategy;
import com.algotradex.model.StrategyState;
import com.algotradex.model.TradeSignal;
import com.algotradex.model.enums.OrderStatus;
import com.algotradex.model.enums.SignalType;
import com.algotradex.repository.StrategyStateRepository;
import com.algotradex.repository.TradeSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyEvaluator {

    private final SignalGenerator signalGenerator;
    private final RiskManager riskManager;
    private final OrderExecutionEngine executionEngine;
    private final StrategyStateRepository stateRepository;
    private final TradeSignalRepository signalRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public void evaluate(Strategy strategy, MarketTickEvent tick) {
        Strategy managedStrategy = entityManager.merge(strategy);
        
        StrategyState state = stateRepository.findByStrategyId(strategy.getId())
                .orElseGet(() -> {
                    StrategyState newState = new StrategyState();
                    newState.setStrategy(managedStrategy);
                    return newState;
                });

        TradeSignal tradeSignal = signalGenerator.evaluateStrategy(strategy, tick);

        if (tradeSignal.getSignalType() == SignalType.NO_TRADE) {
            return; // Fast path out
        }

        tradeSignal.setExecutionStatus(OrderStatus.PENDING);

        if (!riskManager.isTradeAllowed(strategy, state)) {
            tradeSignal.setExecutionStatus(OrderStatus.REJECTED);
            tradeSignal.setRejectionReason("RISK_MANAGEMENT_REJECTED");
            signalRepository.save(tradeSignal);
            return;
        }

        // Fire Execution Engine
        boolean success = executionEngine.executeSignal(strategy, tradeSignal, tick);
        
        if (success) {
            tradeSignal.setExecutionStatus(OrderStatus.COMPLETE);
            state.setDailyTradeCount(state.getDailyTradeCount() + 1);
            stateRepository.save(state);
        } else {
            tradeSignal.setExecutionStatus(OrderStatus.REJECTED);
            tradeSignal.setRejectionReason("EXECUTION_FAILED");
        }
        
        signalRepository.save(tradeSignal);
    }
}
