package com.algotradex.service;

import com.algotradex.dto.auth.AuthResponse;
import com.algotradex.dto.auth.LoginRequest;
import com.algotradex.dto.auth.RegisterRequest;
import com.algotradex.exception.BusinessException;
import com.algotradex.exception.DuplicateResourceException;
import com.algotradex.exception.ResourceNotFoundException;
import com.algotradex.model.RefreshToken;
import com.algotradex.model.User;
import com.algotradex.model.enums.UserRole;
import com.algotradex.model.enums.UserStatus;
import com.algotradex.repository.RefreshTokenRepository;
import com.algotradex.repository.UserRepository;
import com.algotradex.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TwoFactorAuthService twoFactorAuthService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.TRADER)
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .twoFaEnabled(false)
                .build();

        userRepository.save(user);

        // In a real app, send verification email here

        String accessToken = jwtTokenProvider.generateAccessToken(user, user.getId(), user.getRole().name());
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(user);

        saveRefreshToken(user, refreshTokenString);

        return buildAuthResponse(user, accessToken, refreshTokenString);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BusinessException("User account is " + user.getStatus().name());
        }

        if (user.isTwoFaEnabled()) {
            if (request.getTotpCode() == null || request.getTotpCode().isBlank()) {
                AuthResponse response = new AuthResponse();
                response.setRequiresTwoFactor(true);
                return response;
            }
            if (!twoFactorAuthService.isOtpValid(user.getTwoFaSecret(), request.getTotpCode())) {
                throw new BadCredentialsException("Invalid 2FA code");
            }
        }

        // Update login info
        user.setLastLoginAt(ZonedDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        // Revoke existing tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user, user.getId(), user.getRole().name());
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(user);

        saveRefreshToken(user, refreshTokenString, ipAddress, userAgent);

        return buildAuthResponse(user, accessToken, refreshTokenString);
    }

    private void saveRefreshToken(User user, String tokenString, String ipAddress, String userAgent) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenString)
                .ipAddress(ipAddress)
                .deviceInfo(userAgent)
                .expiresAt(ZonedDateTime.now().plusSeconds(jwtTokenProvider.getRefreshExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private void saveRefreshToken(User user, String tokenString) {
        saveRefreshToken(user, tokenString, null, null);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userDto)
                .requiresTwoFactor(false)
                .build();
    }
}
