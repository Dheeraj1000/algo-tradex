package com.algotradex.engine;

import com.algotradex.model.BrokerAccount;
import com.algotradex.repository.BrokerAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealMarketDataStreamer {

    private final ApplicationEventPublisher eventPublisher;
    private final BrokerAccountRepository brokerAccountRepository;
    private final com.algotradex.repository.InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    // Map internal symbols to track
    private final List<String> symbols = Arrays.asList("NIFTY", "SENSEX", "BANKNIFTY");

    @Scheduled(fixedRateString = "${algotradex.real-streamer.rate:10000}")
    public void fetchRealTicks() {
        try {
            // Check if market is open (09:15 to 15:30 IST, Mon-Fri)
            ZoneId istZone = ZoneId.of("Asia/Kolkata");
            ZonedDateTime now = ZonedDateTime.now(istZone);
            int dayOfWeek = now.getDayOfWeek().getValue();
            if (dayOfWeek > 5) {
                log.debug("Market is closed (Weekend). Skipping real tick fetch.");
                return;
            }
            
            int hour = now.getHour();
            int minute = now.getMinute();
            int timeInMinutes = hour * 60 + minute;
            
            // 9:15 = 555 minutes, 15:30 = 930 minutes
            if (timeInMinutes < 555 || timeInMinutes > 930) {
                log.debug("Market is closed (Outside 09:15 - 15:30 IST). Skipping real tick fetch.");
                return;
            }

            // Find an active broker account to use for pulling global data
            BrokerAccount account = brokerAccountRepository.findAll().stream()
                    .filter(a -> a.getStatus() == com.algotradex.model.enums.BrokerStatus.CONNECTED)
                    .filter(a -> a.getBrokerType() == com.algotradex.model.enums.BrokerType.DHAN)
                    .findFirst()
                    .orElse(null);
                    
            if (account == null) {
                return; // Cannot fetch live data without a dhan account
            }
            
            String today = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String url = "https://api.dhan.co/v2/charts/intraday";

            for (String symbol : symbols) {
                String securityId = "13";
                String exchangeSegment = "IDX_I";
                String instrument = "INDEX";
                
                var optionalInst = instrumentRepository.findByTradingSymbol(symbol);
                if (optionalInst.isPresent()) {
                    com.algotradex.model.Instrument inst = optionalInst.get();
                    if (inst.getToken() != null && !inst.getToken().isEmpty()) {
                        securityId = inst.getToken();
                        exchangeSegment = inst.getSegment() != null ? inst.getSegment() : "IDX_I";
                        instrument = inst.getInstrumentType() != null ? inst.getInstrumentType().name() : "INDEX";
                    }
                } else {
                    if ("SENSEX".equals(symbol)) securityId = "51";
                    else if ("BANKNIFTY".equals(symbol)) securityId = "25";
                }
                
                String payload = String.format("{\"securityId\":\"%s\",\"exchangeSegment\":\"%s\",\"instrument\":\"%s\",\"interval\":1,\"oi\":false,\"fromDate\":\"%s\",\"toDate\":\"%s\"}",
                        securityId, exchangeSegment, instrument, today, today);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("client-id", account.getClientId())
                        .header("access-token", account.getAccessToken())
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    if (root.has("close") && root.get("close").size() > 0) {
                        JsonNode closes = root.get("close");
                        JsonNode volumes = root.get("volume");
                        JsonNode timestamps = root.get("timestamp");
                        
                        int lastIdx = closes.size() - 1;
                        BigDecimal price = BigDecimal.valueOf(closes.get(lastIdx).asDouble());
                        long volume = volumes != null && volumes.size() > lastIdx ? volumes.get(lastIdx).asLong(0) : 0;
                        
                        ZonedDateTime tickTime = ZonedDateTime.now();
                        if (timestamps != null && timestamps.size() > lastIdx) {
                            long epochSeconds = timestamps.get(lastIdx).asLong();
                            // Dhan timestamps are in epoch seconds; sometimes they return .0 as float, asLong() handles it.
                            tickTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
                        }
                        
                        // Publish tick for regular symbol
                        MarketTickEvent tick = new MarketTickEvent(symbol, price, volume, tickTime);
                        eventPublisher.publishEvent(tick);
                        
                        // Publish tick for -EQ symbol
                        MarketTickEvent tickEq = new MarketTickEvent(symbol + "-EQ", price, volume, tickTime);
                        eventPublisher.publishEvent(tickEq);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching live ticks from Dhan", e);
        }
    }
}
