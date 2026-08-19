package com.ecommerce.user.service;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.LoginResponse;
import com.ecommerce.user.model.User;
import com.ecommerce.user.model.UserCredentials;
import com.ecommerce.user.model.UserStatus;
import com.ecommerce.user.repository.UserCredentialsRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("AuthService.login() | email={}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("AuthService.login() | user not found | email={}", request.email());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (user.getStatus() == UserStatus.DELETED || user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("AuthService.login() | account disabled | userId={} | status={}", user.getUserId(), user.getStatus());
            throw new DisabledException("Account is " + user.getStatus().name().toLowerCase());
        }

        UserCredentials credentials = credentialsRepository
                .findByUser_UserId(user.getUserId())
                .orElseThrow(() -> {
                    log.warn("AuthService.login() | no credentials found | userId={}", user.getUserId());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), credentials.getPassword())) {
            log.warn("AuthService.login() | wrong password | userId={}", user.getUserId());
            throw new BadCredentialsException("Invalid email or password");
        }

        List<String> roles = List.of("BUYER");
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), roles);

        log.info("AuthService.login() | login successful | userId={}", user.getUserId());
        return new LoginResponse(token, "Bearer", expirationMs, user.getUserId(), user.getEmail());
    }
}
