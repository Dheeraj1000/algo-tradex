package com.algotradex.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@ConditionalOnProperty(name = "algotradex.marketdata.provider", havingValue = "mock")
public class MockMarketDataStreamer {

    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();
    private final Map<String, BigDecimal> currentPrices = new HashMap<>();

    public MockMarketDataStreamer(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        currentPrices.put("RELIANCE", new BigDecimal("2488.50"));
        currentPrices.put("RELIANCE-EQ", new BigDecimal("2488.50"));
        currentPrices.put("HDFCBANK", new BigDecimal("1422.30"));
        currentPrices.put("HDFCBANK-EQ", new BigDecimal("1422.30"));
        currentPrices.put("SBIN", new BigDecimal("782.10"));
        currentPrices.put("SBIN-EQ", new BigDecimal("782.10"));
        currentPrices.put("TCS", new BigDecimal("3845.20"));
        currentPrices.put("TCS-EQ", new BigDecimal("3845.20"));
        currentPrices.put("INFY", new BigDecimal("1498.40"));
        currentPrices.put("INFY-EQ", new BigDecimal("1498.40"));
        currentPrices.put("ICICIBANK", new BigDecimal("1150.00"));
        currentPrices.put("ICICIBANK-EQ", new BigDecimal("1150.00"));
        currentPrices.put("AXISBANK", new BigDecimal("1080.00"));
        currentPrices.put("AXISBANK-EQ", new BigDecimal("1080.00"));
        currentPrices.put("BHARTIAIRTEL", new BigDecimal("1210.00"));
        currentPrices.put("BHARTIAIRTEL-EQ", new BigDecimal("1210.00"));
        currentPrices.put("BHARTIARTL", new BigDecimal("1210.00"));
        currentPrices.put("BHARTIARTL-EQ", new BigDecimal("1210.00"));
        currentPrices.put("ITC", new BigDecimal("430.00"));
        currentPrices.put("ITC-EQ", new BigDecimal("430.00"));
        currentPrices.put("LT", new BigDecimal("3450.00"));
        currentPrices.put("LT-EQ", new BigDecimal("3450.00"));
        currentPrices.put("KOTAKBANK", new BigDecimal("1810.00"));
        currentPrices.put("KOTAKBANK-EQ", new BigDecimal("1810.00"));
        currentPrices.put("NIFTYBEES", new BigDecimal("250.00"));
        currentPrices.put("NIFTYBEES-EQ", new BigDecimal("250.00"));
        currentPrices.put("NIFTY50", new BigDecimal("22500.00"));
    }

    @Scheduled(fixedRateString = "${algotradex.mock-streamer.rate:2000}")
    public void generateFakeTicks() {
        for (Map.Entry<String, BigDecimal> entry : currentPrices.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal price = entry.getValue();

            // Random walk: change price by -0.2% to +0.2%
            double changePercent = (random.nextDouble() * 0.4) - 0.2;
            BigDecimal change = price.multiply(BigDecimal.valueOf(changePercent / 100.0));
            BigDecimal newPrice = price.add(change).setScale(2, RoundingMode.HALF_UP);
            currentPrices.put(symbol, newPrice);

            long volume = random.nextInt(500) + 1;

            MarketTickEvent tick = new MarketTickEvent(symbol, newPrice, volume, ZonedDateTime.now());
            // log.debug("Mock Tick -> {}", tick);
            eventPublisher.publishEvent(tick);
        }
    }
}
