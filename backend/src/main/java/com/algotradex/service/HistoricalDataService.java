package com.algotradex.service;

import com.algotradex.dto.CandleDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataService {

    private final com.algotradex.repository.InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    /**
     * Fetch historical OHLC candles from DhanHQ.
     * @param symbol internal symbol (e.g., NIFTY)
     * @param start start time
     * @param end end time
     * @param interval interval (e.g., 1m, 5m)
     * @param brokerAccount User's Dhan credentials
     * @return List of CandleDTO
     */
    public List<CandleDTO> fetchHistoricalData(String symbol, ZonedDateTime start, ZonedDateTime end, String interval, com.algotradex.model.BrokerAccount brokerAccount) {
        List<CandleDTO> candles = new ArrayList<>();
        
        try {
            if (brokerAccount.getBrokerType() == com.algotradex.model.enums.BrokerType.MOCK) {
                // Generate 100 mock candles for the chart
                long now = end.toEpochSecond();
                double basePrice = 1000.0;
                
                if (symbol.contains("NIFTY")) basePrice = 24000.0;
                else if (symbol.contains("BANK")) basePrice = 50000.0;
                else if (symbol.contains("RELIANCE")) basePrice = 2500.0;
                else if (symbol.contains("HDFC")) basePrice = 1400.0;
                else if (symbol.contains("SBI")) basePrice = 750.0;

                for (int i = 100; i >= 0; i--) {
                    long timeVal = now - (i * 60L);
                    double open = basePrice + (Math.random() * 10 - 5);
                    double high = open + (Math.random() * 5);
                    double low = open - (Math.random() * 5);
                    double close = open + (Math.random() * 8 - 4);
                    
                    candles.add(CandleDTO.builder()
                            .time(timeVal)
                            .open(BigDecimal.valueOf(open))
                            .high(BigDecimal.valueOf(high))
                            .low(BigDecimal.valueOf(low))
                            .close(BigDecimal.valueOf(close))
                            .volume((long)(Math.random() * 1000 + 100))
                            .build());
                    basePrice = close;
                }
                return candles;
            }

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
                String base = symbol.replace("-EQ", "").toUpperCase();
                if (base.equals("SENSEX")) {
                    securityId = "51";
                } else if (base.equals("BANKNIFTY") || base.equals("NIFTY BANK")) {
                    securityId = "25";
                } else if (!base.startsWith("NIFTY")) {
                    instrument = "EQUITY";
                    exchangeSegment = "NSE_EQ";
                }
            }
            
            String fromDate = start.toLocalDate().toString();
            String toDate = end.toLocalDate().toString();
            
            String url = "https://api.dhan.co/v2/charts/intraday";
            
            String payload = String.format("{\"securityId\":\"%s\",\"exchangeSegment\":\"%s\",\"instrument\":\"%s\",\"interval\":1,\"oi\":false,\"fromDate\":\"%s\",\"toDate\":\"%s\"}",
                    securityId, exchangeSegment, instrument, fromDate, toDate);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("client-id", brokerAccount.getClientId())
                    .header("access-token", brokerAccount.getAccessToken())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("timestamp")) {
                    JsonNode timestamps = root.get("timestamp");
                    JsonNode opens = root.get("open");
                    JsonNode highs = root.get("high");
                    JsonNode lows = root.get("low");
                    JsonNode closes = root.get("close");
                    JsonNode volumes = root.get("volume");
                    
                    for (int i = 0; i < timestamps.size(); i++) {
                        long timeVal = timestamps.get(i).asLong();
                        
                        CandleDTO candle = CandleDTO.builder()
                                .time(timeVal)
                                .open(BigDecimal.valueOf(opens.get(i).asDouble()))
                                .high(BigDecimal.valueOf(highs.get(i).asDouble()))
                                .low(BigDecimal.valueOf(lows.get(i).asDouble()))
                                .close(BigDecimal.valueOf(closes.get(i).asDouble()))
                                .volume(volumes.get(i).asLong())
                                .build();
                        candles.add(candle);
                    }
                } else {
                    log.warn("Dhan API returned unexpected JSON: {}", response.body());
                }
            } else {
                log.warn("Failed to fetch historical data from Dhan. Status: {}", response.statusCode());
            }
            
        } catch (Exception e) {
            log.error("Error fetching historical data from Dhan", e);
        }
        
        return candles;
    }

    /**
     * Fetch Live Traded Price (LTP) from Binance Public API.
     */
    public BigDecimal getBinanceLtp(String symbol) {
        try {
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + symbol;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return new BigDecimal(root.get("price").asText());
            } else {
                log.warn("Failed to fetch Binance LTP for {}. Status: {}", symbol, response.statusCode());
            }
        } catch (Exception e) {
            log.error("Error fetching Binance LTP for {}", symbol, e);
        }
        return null;
    }
}
