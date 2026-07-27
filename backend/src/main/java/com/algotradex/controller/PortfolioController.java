package com.algotradex.controller;

import com.algotradex.model.Position;
import com.algotradex.model.User;
import com.algotradex.repository.UserRepository;
import com.algotradex.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.Data;
import lombok.Builder;
import com.algotradex.model.enums.PositionStatus;
import com.algotradex.model.enums.TradingMode;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    @GetMapping("/positions")
    public ResponseEntity<List<Position>> getPositions(
            @AuthenticationPrincipal UserDetails userDetails) {
            
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        return ResponseEntity.ok(portfolioService.getPositions(user.getId()));
    }

    @Data
    @Builder
    public static class IndianStatsDto {
        private Double accountBalance;
        private Double realizedPnl;
        private Double unrealizedPnl;
    }

    @GetMapping("/indian-stats")
    public ResponseEntity<IndianStatsDto> getIndianStats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Position> positions = portfolioService.getPositions(user.getId());
        
        double startingCapital = 100000.0;
        double totalRealized = 0.0;
        double totalUnrealized = 0.0;
        double investedMargin = 0.0;

        for (Position pos : positions) {
            if (pos.getTradingMode() == TradingMode.PAPER) {
                if (pos.getStatus() == PositionStatus.CLOSED) {
                    if (pos.getRealizedPnl() != null) {
                        totalRealized += pos.getRealizedPnl().doubleValue();
                    }
                } else if (pos.getStatus() == PositionStatus.OPEN) {
                    if (pos.getUnrealizedPnl() != null) {
                        totalUnrealized += pos.getUnrealizedPnl().doubleValue();
                    }
                    if (pos.getAvgEntryPrice() != null) {
                        double lotSize = 1.0;
                        String symbol = pos.getInstrument().getTradingSymbol();
                        if (symbol.startsWith("^NSEI")) lotSize = 50.0;
                        else if (symbol.startsWith("^BSESN")) lotSize = 10.0;
                        
                        investedMargin += pos.getAvgEntryPrice().doubleValue() * pos.getQuantity() * lotSize;
                    }
                }
            }
        }

        double accountBalance = startingCapital + totalRealized - investedMargin;

        return ResponseEntity.ok(IndianStatsDto.builder()
                .accountBalance(accountBalance)
                .realizedPnl(totalRealized)
                .unrealizedPnl(totalUnrealized)
                .build());
    }
}
