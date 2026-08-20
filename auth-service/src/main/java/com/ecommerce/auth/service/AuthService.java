package com.ecommerce.auth.service;

import com.ecommerce.auth.client.UserServiceClient;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.LoginResponse;
import com.ecommerce.auth.dto.UserCredentialsView;
import com.ecommerce.auth.exception.AccountNotActiveException;
import com.ecommerce.auth.exception.InvalidCredentialsException;
import com.ecommerce.auth.security.JwtTokenProvider;
import feign.FeignException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class AuthService {

    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    

    public LoginResponse login(LoginRequest request) {
        UserCredentialsView credentials = fetchCredentials(request.email());

        if (!passwordEncoder.matches(request.password(), credentials.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!"ACTIVE".equals(credentials.status())) {
            throw new AccountNotActiveException(credentials.status());
        }

        return issueTokens(credentials.userId(), credentials.email(), credentials.roles());
    }

    /**
     * Rotates a refresh token for a new access/refresh pair. Re-checks the
     * user's current status so a suspended/deleted account can't keep
     * refreshing its way to new access tokens.
     */
    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parseAndValidate(refreshToken);
        } catch (JwtException ex) {
            throw ex; // handled by GlobalExceptionHandler -> 401
        }

        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new InvalidCredentialsException();
        }

        UUID userId = UUID.fromString(claims.getSubject());
        // Re-fetch current state — roles/status may have changed since the
        // refresh token was issued.
        UserCredentialsView credentials = fetchCredentialsById(userId);

        if (!"ACTIVE".equals(credentials.status())) {
            throw new AccountNotActiveException(credentials.status());
        }

        return issueTokens(credentials.userId(), credentials.email(), credentials.roles());
    }

    private LoginResponse issueTokens(UUID userId, String email, List<String> roles) {
        String accessToken = tokenProvider.createAccessToken(userId, email, roles);
        String refreshToken = tokenProvider.createRefreshToken(userId);
        return LoginResponse.bearer(accessToken, refreshToken, tokenProvider.accessTokenTtlSeconds());
    }

    private UserCredentialsView fetchCredentials(String email) {
        try {
            return userServiceClient.findCredentialsByEmail(email);
        } catch (FeignException.NotFound ex) {
            // Same exception as a wrong password — don't reveal whether the
            // email exists.
            throw new InvalidCredentialsException();
        }
    }

    private UserCredentialsView fetchCredentialsById(UUID userId) {
        try {
            return userServiceClient.findCredentialsByUserId(userId);
        } catch (FeignException.NotFound ex) {
            throw new InvalidCredentialsException();
        }
    }
}
