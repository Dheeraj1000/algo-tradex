package com.algotradex.service;

import com.algotradex.model.Strategy;
import com.algotradex.model.User;
import com.algotradex.model.enums.StrategyStatus;
import com.algotradex.repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRepository strategyRepository;

    public List<Strategy> getUserStrategies(UUID userId) {
        return strategyRepository.findByUserId(userId);
    }

    @Transactional
    public Strategy createStrategy(User user, Strategy strategy) {
        strategy.setUser(user);
        if (strategy.getStatus() == null) {
            strategy.setStatus(StrategyStatus.DRAFT);
        }
        return strategyRepository.save(strategy);
    }

    public Strategy getStrategy(UUID strategyId, UUID userId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("Strategy not found"));
        if (!strategy.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to strategy");
        }
        return strategy;
    }

    @Transactional
    public Strategy updateStrategy(UUID strategyId, Strategy updated, UUID userId) {
        Strategy strategy = getStrategy(strategyId, userId);
        strategy.setName(updated.getName());
        strategy.setDescription(updated.getDescription());
        strategy.setMaxDailyLoss(updated.getMaxDailyLoss());
        strategy.setMaxExposure(updated.getMaxExposure());
        strategy.setAiThreshold(updated.getAiThreshold());
        strategy.setPaperTrading(updated.isPaperTrading());
        strategy.setConfig(updated.getConfig());
        
        // If status was toggled as part of update
        if (updated.getStatus() != null) {
            strategy.setStatus(updated.getStatus());
        }
        
        return strategyRepository.save(strategy);
    }

    @Transactional
    public void deleteStrategy(UUID strategyId, UUID userId) {
        Strategy strategy = getStrategy(strategyId, userId);
        strategyRepository.delete(strategy);
    }

    @Transactional
    public Strategy toggleStrategy(UUID strategyId, UUID userId) {
        Strategy strategy = getStrategy(strategyId, userId);
        if (strategy.getStatus() == StrategyStatus.ACTIVE) {
            strategy.setStatus(StrategyStatus.PAUSED);
        } else {
            strategy.setStatus(StrategyStatus.ACTIVE);
        }
        return strategyRepository.save(strategy);
    }
}
