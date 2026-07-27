package com.algotradex.controller;

import com.algotradex.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthData = Map.of(
                "status", "UP",
                "service", "AlgoTradeX Backend",
                "version", "1.0.0",
                "timestamp", ZonedDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.success("Service is healthy", healthData));
    }
}
