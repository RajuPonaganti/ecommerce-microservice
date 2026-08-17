package com.ecommerce.user.service;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.LoginResponse;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.model.User;
import com.ecommerce.user.model.UserCredentials;
import com.ecommerce.user.model.UserStatus;
import com.ecommerce.user.repository.UserCredentialsRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // 1. Find user by email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // 2. Reject suspended / deleted accounts
        if (user.getStatus() == UserStatus.DELETED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new DisabledException("Account is " + user.getStatus().name().toLowerCase());
        }

        // 3. Load credentials and verify password
        UserCredentials credentials = credentialsRepository
                .findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), credentials.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // 4. Build roles — extend this when role table is added
        List<String> roles = List.of("BUYER");

        // 5. Generate JWT
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), roles);

        return new LoginResponse(
                token,
                "Bearer",
                expirationMs,
                user.getUserId(),
                user.getEmail()
        );
    }
}
