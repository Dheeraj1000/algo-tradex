package com.algotradex.controller;

import com.algotradex.model.BrokerAccount;
import com.algotradex.model.User;
import com.algotradex.model.enums.BrokerType;
import com.algotradex.repository.UserRepository;
import com.algotradex.service.BrokerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brokers")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerService brokerService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<BrokerAccount> linkBroker(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LinkBrokerRequest request) {
            
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BrokerAccount account = brokerService.linkBrokerAccount(
                user, request.getBrokerType(), request.getClientId(), request.getApiKey(), request.getApiSecret(), request.getPin(), request.getTotpSecret(), request.getAccessToken());
                
        BrokerAccount connectedAccount = brokerService.connectBroker(account.getId());
        
        return ResponseEntity.ok(connectedAccount);
    }
    
    @GetMapping
    public ResponseEntity<List<BrokerAccount>> getBrokerAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
            
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        return ResponseEntity.ok(brokerService.getUserBrokerAccounts(user.getId()));
    }

    @Data
    static class LinkBrokerRequest {
        private BrokerType brokerType;
        private String clientId;
        private String apiKey;
        private String apiSecret;
        private String pin;
        private String totpSecret;
        private String accessToken;
    }
}
