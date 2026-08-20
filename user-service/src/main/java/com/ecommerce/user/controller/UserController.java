package com.ecommerce.user.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.user.dto.AddressDto;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.dto.SetPasswordRequest;
import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UserCredentialsView;
import com.ecommerce.user.dto.UserDto;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.model.User;
import com.ecommerce.user.model.UserCredentials;
import com.ecommerce.user.repository.UserCredentialsRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final UserRepository userRepository;
    private final UserCredentialsRepository credentialsRepository;

   

    @GetMapping("/credentials")
    public ResponseEntity<UserCredentialsView> findByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(this::toView)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/credentials")
    public ResponseEntity<UserCredentialsView> findByUserId(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(this::toView)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private UserCredentialsView toView(User user) {
        UserCredentials creds = credentialsRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new UserNotFoundException(user.getUserId()));

        // Placeholder role derivation — swap in your real roles/authorities
        // source (a roles table, claims from a prior IdP, etc.) once you have one.
        List<String> roles = List.of("ROLE_USER");

        return new UserCredentialsView(
                user.getUserId(),
                user.getEmail(),
                creds.getPassword(),
                user.getStatus().name(),
                user.isMfaEnabled(),
                roles
        );
    }

    /**
     * POST /v1/users/register
     * Register a new user. No authentication required.
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        UserDto created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /v1/users/{userId}
     * Fetch a user by ID. Requires JWT (self or ADMIN).
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    /**
     * PATCH /v1/users/{userId}
     * Update display name or phone. Requires JWT (self).
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    /**
     * DELETE /v1/users/{userId}
     * Soft-delete a user. Requires JWT (self or ADMIN).
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.accepted().build(); // 202 Accepted
    }

    /**
     * POST /v1/users/{userId}/addresses
     * Add an address for a user. Requires JWT (self).
     */
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<AddressDto> addAddress(
            @PathVariable UUID userId,
            @Valid @RequestBody AddressDto addressDto) {
        AddressDto created = userService.addAddress(userId, addressDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH /v1/users/{userId}/password
     * Set or update the password for a user. Requires JWT (self).
     * Returns 204 No Content on success — never echo passwords back.
     */
    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> setPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody SetPasswordRequest request) {
        userService.setPassword(userId, request.password());
        return ResponseEntity.noContent().build(); // 204
    }

    /**
     * GET /v1/users/{userId}/addresses
     * Get all addresses for a user. Requires JWT (self).
     */
    @GetMapping("/{userId}/addresses")
    public ResponseEntity<List<AddressDto>> getAddresses(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getAddresses(userId));
    }
}
