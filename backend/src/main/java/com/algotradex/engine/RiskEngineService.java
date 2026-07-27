package com.algotradex.engine;

import com.algotradex.model.Strategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RiskEngineService {

    public boolean canEnterTrade(Strategy strategy, double potentialExposure) {
        // Implement complex risk checks: max daily loss, max exposure, margin
        if (strategy.getMaxExposure() != null && strategy.getMaxExposure().doubleValue() < potentialExposure) {
            log.warn("Risk Engine: Trade blocked for strategy {} due to Max Exposure limit.", strategy.getId());
            return false;
        }
        
        // Check daily loss limit, etc. (Stub for now)
        return true;
    }
    
    public void emergencyKillSwitch(Strategy strategy) {
        log.error("EMERGENCY KILL SWITCH TRIGGERED for Strategy: {}", strategy.getName());
        // Logic to immediately exit all positions for this strategy
    }
}
