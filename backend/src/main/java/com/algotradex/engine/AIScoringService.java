package com.algotradex.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class AIScoringService {

    private final Random random = new Random();

    /**
     * Mocks an API call to an external Python FastAPI ML model.
     * Returns a confidence score between 0.0 and 100.0
     */
    public double evaluateConfidence(String symbol) {
        // In a real scenario, this would format the last N candles and send them to the AI model
        double score = random.nextDouble() * 100.0;
        // log.debug("AI Evaluation for {}: Confidence {}%", symbol, String.format("%.2f", score));
        return score;
    }
}
