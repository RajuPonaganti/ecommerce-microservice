package com.ecommerce.user.controller;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.LoginResponse;
import com.ecommerce.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /v1/auth/login
     * Validates email + password against stored (hashed) credentials.
     * Returns a signed JWT on success.
     *
     * Request body:
     *   { "email": "user@example.com", "password": "secret123" }
     *
     * Response 200:
     *   { "accessToken": "eyJ...", "tokenType": "Bearer",
     *     "expiresInMs": 86400000, "userId": "...", "email": "..." }
     *
     * Response 401: invalid credentials or disabled account
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException | DisabledException ex) {
            // Return 401 with a plain message — never reveal which part was wrong
            return ResponseEntity.status(401)
                    .body("{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }
}
