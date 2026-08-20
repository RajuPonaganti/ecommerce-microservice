package com.ecommerce.api.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global JWT validation filter.
 *
 * <p>Runs at order -2 (before {@link LoggingGlobalFilter}) so an invalid token
 * is rejected before any logging or routing occurs.
 *
 * <h3>What it does</h3>
 * <ol>
 *   <li>Skips public paths (configured via {@code gateway.jwt.public-paths}).</li>
 *   <li>Extracts the Bearer token from {@code Authorization} header.</li>
 *   <li>Validates the token signature and expiry using the shared HMAC secret.</li>
 *   <li>On success: forwards {@code X-Auth-User} and {@code X-Auth-Roles} headers
 *       to downstream services so they don't need to re-validate.</li>
 *   <li>On failure: returns {@code 401 Unauthorized} immediately.</li>
 * </ol>
 *
 * <h3>Configuration (application.yml)</h3>
 * <pre>
 * gateway:
 *   jwt:
 *     secret: "your-256-bit-or-longer-secret-here"
 *     public-paths:
 *       - /actuator/**
 *       - /auth/**
 * </pre>
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;
    private final List<String> publicPaths;

  
    
    public JwtAuthFilter(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.publicPaths = jwtProperties.getPublicPaths();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip JWT check for public paths (e.g. /auth/login, /actuator/health)
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return unauthorised(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Forward useful claims as headers so downstream services
            // can read them without having to re-validate the JWT.
            String subject = claims.getSubject();
            String roles   = claims.get("roles", String.class);   // e.g. "ROLE_USER,ROLE_ADMIN"

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-Auth-User",  subject != null ? subject : "")
                    .header("X-Auth-Roles", roles   != null ? roles   : "")
                    .build();

            log.debug("JWT validated – subject={} path={}", subject, path);
            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException ex) {
            log.warn("JWT validation failed for path {}: {}", path, ex.getMessage());
            return unauthorised(exchange);
        }
    }

    @Override
    public int getOrder() {
        // Must run before LoggingGlobalFilter (order -1).
        return -2;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Simple prefix/glob matcher for public paths.
     * Supports exact paths and paths ending with {@code /**}.
     */
    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                return path.startsWith(prefix);
            }
            return path.equals(pattern);
        });
    }

    private Mono<Void> unauthorised(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // Prevent downstream filters from sending a body
        return response.setComplete();
    }
}
