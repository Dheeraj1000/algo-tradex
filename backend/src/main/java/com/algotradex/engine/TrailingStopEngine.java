package com.algotradex.engine;

import com.algotradex.model.TradeSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TrailingStopEngine {

    public Double calculateTrailingStop(TradeSignal signal, double currentPrice) {
        // High-Water Mark Trailing Stop Loss
        // When AI calls MOVE_STOP_LOSS, we set a tight 10% trailing stop behind current peak price
        double tightStop = currentPrice * 0.90; 
        
        if (signal.getCurrentStopLoss() == null) {
            return tightStop;
        }
        
        // Only move stop loss UP, never down
        if (tightStop > signal.getCurrentStopLoss()) {
            return tightStop;
        }
        return signal.getCurrentStopLoss();
    }
}
