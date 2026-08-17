package com.ecommerce.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        // Key must be >= 256 bits for HS256
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed JWT token.
     *
     * @param userId  subject claim (user's UUID)
     * @param email   email claim
     * @param roles   list of role strings e.g. ["BUYER", "SELLER"]
     */
    public String generateToken(UUID userId, String email, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())          // "sub" claim
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)                       // "iat" claim
                .expiration(expiry)                  // "exp" claim
                .signWith(secretKey)                 // HS256 by default with SecretKey
                .compact();
    }

    /** Extract the subject (userId) from a token. */
    public String extractSubject(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /** Validate a token — throws JwtException if invalid or expired. */
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
