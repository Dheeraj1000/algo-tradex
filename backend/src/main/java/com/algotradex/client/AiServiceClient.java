package com.algotradex.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Data
    public static class PredictionResponse {
        private String status;
        private String symbol;
        private String target_symbol;
        private Double confidence_score;
        private String signal;
        private String message;
    }

    public Optional<PredictionResponse> getPrediction(String symbol, String clientId, String accessToken) {
        try {
            String url = aiServiceUrl + "/models/predict";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Format request body
            String requestBody = "{\"symbol\": \"" + symbol + "\"";
            if (clientId != null && accessToken != null) {
                requestBody += ", \"client_id\": \"" + clientId + "\", \"access_token\": \"" + accessToken + "\"";
            }
            requestBody += "}";
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            PredictionResponse response = restTemplate.postForObject(url, request, PredictionResponse.class);
            
            if (response != null && "success".equals(response.getStatus())) {
                return Optional.of(response);
            } else {
                log.warn("AI Service returned error or missing data for symbol {}: {}", symbol, 
                        response != null ? response.getMessage() : "null response");
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to connect to AI service for prediction on {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }
    
    public boolean trainModel(String symbol, String clientId, String accessToken) {
        try {
            String url = aiServiceUrl + "/models/train";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String requestBody = "{\"symbol\": \"" + symbol + "\"";
            if (clientId != null && accessToken != null) {
                requestBody += ", \"client_id\": \"" + clientId + "\", \"access_token\": \"" + accessToken + "\"";
            }
            requestBody += "}";
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            Map response = restTemplate.postForObject(url, request, Map.class);
            return response != null && "success".equals(response.get("status"));
        } catch (Exception e) {
            log.error("Failed to trigger AI training for {}: {}", symbol, e.getMessage());
            return false;
        }
    }

    @Data
    public static class ManageTradeRequest {
        private String symbol;
        private String target_option;
        private String signal_type;
        private double entry_price;
        private double current_price;
        private double stop_loss;
        private double target;
        private int quantity;
        private boolean stop_loss_breached;
        private String client_id;
        private String access_token;
    }

    @Data
    public static class ManageTradeResponse {
        private String status;
        private String action; // HOLD, EXIT, PARTIAL_EXIT, MOVE_STOP_LOSS, REENTER
        private Double recovery_probability;
        private String explanation;
    }

    public Optional<ManageTradeResponse> manageTrade(ManageTradeRequest request) {
        try {
            String url = aiServiceUrl + "/models/manage_trade";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<ManageTradeRequest> entity = new HttpEntity<>(request, headers);
            ManageTradeResponse response = restTemplate.postForObject(url, entity, ManageTradeResponse.class);
            
            if (response != null && "success".equals(response.getStatus())) {
                return Optional.of(response);
            } else {
                log.warn("AI Service returned error for manage_trade: {}", response);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to connect to AI service for trade management: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
