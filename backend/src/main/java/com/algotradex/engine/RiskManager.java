package com.algotradex.engine;

import com.algotradex.model.Strategy;
import com.algotradex.model.StrategyState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class RiskManager {

    private boolean globalKillSwitchActive = false;

    public void activateKillSwitch() {
        this.globalKillSwitchActive = true;
        log.error("GLOBAL KILL SWITCH ACTIVATED!");
    }

    public boolean isTradeAllowed(Strategy strategy, StrategyState state) {
        if (globalKillSwitchActive) {
            log.warn("RiskManager: Trade rejected by global kill switch");
            return false;
        }

        if (strategy.getMaxDailyLoss() != null && state.getDailyRealizedPnL() != null) {
            // Check if daily realized PnL is worse than max daily loss
            // maxDailyLoss is usually positive (e.g. 5000), realizedPnL is negative (e.g. -5500)
            if (state.getDailyRealizedPnL().compareTo(strategy.getMaxDailyLoss().negate()) < 0) {
                log.warn("RiskManager: Trade rejected, max daily loss breached for strategy {}", strategy.getName());
                return false;
            }
        }

        // Add additional checks here: Max trades per day, max exposure, etc.
        if (state.getDailyTradeCount() > 50) {
            log.warn("RiskManager: Trade rejected, max daily trade count reached for strategy {}", strategy.getName());
            return false;
        }

        return true;
    }
}
