package com.ecommerce.auth.client;

import com.ecommerce.auth.dto.UserCredentialsView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Talks to user-service directly via the Eureka-registered service id
 * (bypasses the API gateway — internal service-to-service call).
 * <p>
 * user-service returns 404 when no match is found; that surfaces here as a
 * {@link feign.FeignException.NotFound}, which {@code AuthService} catches
 * and translates into {@code InvalidCredentialsException} so callers never
 * learn whether it was the email or the password that was wrong.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/v1/users/credentials")
    UserCredentialsView findCredentialsByEmail(@RequestParam("email") String email);

    @GetMapping("/v1/users/{userId}/credentials")
    UserCredentialsView findCredentialsByUserId(@PathVariable("userId") UUID userId);
}
