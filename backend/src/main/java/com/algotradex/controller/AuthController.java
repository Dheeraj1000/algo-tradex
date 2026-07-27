package com.algotradex.controller;

import com.algotradex.dto.ApiResponse;
import com.algotradex.dto.auth.AuthResponse;
import com.algotradex.dto.auth.LoginRequest;
import com.algotradex.dto.auth.RegisterRequest;
import com.algotradex.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("User registered successfully")
                .data(response)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        
        AuthResponse response = authService.login(request, ipAddress, userAgent);
        
        if (response.isRequiresTwoFactor()) {
            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .success(true)
                    .message("2FA verification required")
                    .data(response)
                    .build());
        }
        
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build());
    }
}
