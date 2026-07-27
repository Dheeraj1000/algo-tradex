package com.algotradex.engine;

import com.algotradex.model.Strategy;
import com.algotradex.model.enums.StrategyStatus;
import com.algotradex.repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyOrchestrator {

    private final StrategyRepository strategyRepository;
    private final StrategyEvaluator strategyEvaluator;

    @Async
    @EventListener
    public void onMarketTick(MarketTickEvent tick) {
        // In a production system, strategies would be cached in memory (Redis/Local Cache)
        // rather than queried from the DB on every tick.
        List<Strategy> activeStrategies = strategyRepository.findAll().stream()
                .filter(s -> s.getStatus() == StrategyStatus.ACTIVE)
                .collect(Collectors.toList());

        for (Strategy strategy : activeStrategies) {
            try {
                strategyEvaluator.evaluate(strategy, tick);
            } catch (Exception e) {
                log.error("Error evaluating strategy {}", strategy.getId(), e);
            }
        }
    }
}
