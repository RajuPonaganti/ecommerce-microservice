package com.paymentgateway.config;

import com.paymentgateway.security.ApiKeyAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ApiKeyAuthFilter apiKeyAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no auth needed
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/actuator/**",
                                "/api/v1/banks/**"
                        ).permitAll()
                        // Everything else goes through ApiKeyAuthFilter
                        .anyRequest().permitAll()
                )
                // Insert our filter BEFORE Spring's own UsernamePasswordAuthenticationFilter
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
