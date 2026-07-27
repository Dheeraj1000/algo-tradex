package com.algotradex.controller;

import com.algotradex.model.Strategy;
import com.algotradex.model.User;
import com.algotradex.repository.UserRepository;
import com.algotradex.service.StrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;
    private final UserRepository userRepository;
    private final com.algotradex.engine.BacktestEngine backtestEngine;

    @GetMapping
    public ResponseEntity<List<Strategy>> getStrategies(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(strategyService.getUserStrategies(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Strategy> createStrategy(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Strategy strategy) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(strategyService.createStrategy(user, strategy));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Strategy> getStrategy(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") UUID id) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(strategyService.getStrategy(id, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Strategy> updateStrategy(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") UUID id,
            @RequestBody Strategy strategy) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(strategyService.updateStrategy(id, strategy, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStrategy(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") UUID id) {
        User user = getUser(userDetails);
        strategyService.deleteStrategy(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Strategy> toggleStrategy(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") UUID id) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(strategyService.toggleStrategy(id, user.getId()));
    }

    @PostMapping("/{id}/backtest")
    public ResponseEntity<com.algotradex.dto.BacktestResult> runBacktest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") UUID id,
            @RequestBody com.algotradex.dto.BacktestRequest request) {
        User user = getUser(userDetails);
        Strategy strategy = strategyService.getStrategy(id, user.getId());
        
        java.time.ZonedDateTime start = java.time.ZonedDateTime.parse(request.getStartDate());
        java.time.ZonedDateTime end = java.time.ZonedDateTime.parse(request.getEndDate());
        
        com.algotradex.dto.BacktestResult result = backtestEngine.runBacktest(
                strategy, start, end, request.getInterval(), request.getInitialCapital());
                
        return ResponseEntity.ok(result);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
