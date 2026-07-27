package com.algotradex.dto.auth;

import com.algotradex.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserDto user;
    private boolean requiresTwoFactor;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private UserRole role;
    }
}
