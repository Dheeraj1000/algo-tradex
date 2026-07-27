package com.algotradex.controller;

import com.algotradex.dto.CandleDTO;
import com.algotradex.model.User;
import com.algotradex.model.BrokerAccount;
import com.algotradex.repository.UserRepository;
import com.algotradex.repository.BrokerAccountRepository;
import com.algotradex.service.HistoricalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.algotradex.dto.AiAlertDto;
import com.algotradex.repository.BrokerAccountRepository;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final HistoricalDataService historicalDataService;
    private final UserRepository userRepository;
    private final BrokerAccountRepository brokerAccountRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.algotradex.engine.CryptoStrategyEvaluator cryptoStrategyEvaluator;

    @GetMapping("/candles")
    public ResponseEntity<List<CandleDTO>> getHistoricalCandles(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        
        // Resolve user and broker account
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        BrokerAccount brokerAccount = brokerAccountRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                .stream()
                .filter(a -> a.getStatus() == com.algotradex.model.enums.BrokerStatus.CONNECTED)
                .sorted((a, b) -> a.getBrokerType() == com.algotradex.model.enums.BrokerType.DHAN ? -1 : 1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active BrokerAccount found to fetch Dhan data"));
                
        // For realtime charts, fetch last 1 day of 1m candles
        ZonedDateTime end = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime start = end.minusDays(5);
        
        List<CandleDTO> candles = historicalDataService.fetchHistoricalData(symbol, start, end, "1m", brokerAccount);
        
        // Return latest `limit` candles
        if (candles.size() > limit) {
            candles = candles.subList(candles.size() - limit, candles.size());
        }
        
        return ResponseEntity.ok(candles);
    }
    @GetMapping("/public-candles")
    public ResponseEntity<List<CandleDTO>> getPublicCandles(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "limit", defaultValue = "400") int limit) {
            
        // Use a hardcoded broker account to test
        BrokerAccount brokerAccount = brokerAccountRepository.findAll().stream()
                .filter(a -> a.getStatus() == com.algotradex.model.enums.BrokerStatus.CONNECTED)
                .sorted((a, b) -> a.getBrokerType() == com.algotradex.model.enums.BrokerType.DHAN ? -1 : 1)
                .findFirst().orElseThrow();
                
        ZonedDateTime end = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime start = end.minusDays(5);
        
        List<CandleDTO> candles = historicalDataService.fetchHistoricalData(symbol, start, end, "1m", brokerAccount);
        
        if (candles.size() > limit) {
            candles = candles.subList(candles.size() - limit, candles.size());
        }
        
        return ResponseEntity.ok(candles);
    }
    
    @PostMapping("/ai-alerts")
    public ResponseEntity<Void> receiveAiAlert(@RequestBody AiAlertDto alert) {
        messagingTemplate.convertAndSend("/topic/ai-alerts", alert);
        cryptoStrategyEvaluator.evaluateAlert(alert);
        return ResponseEntity.ok().build();
    }
}


