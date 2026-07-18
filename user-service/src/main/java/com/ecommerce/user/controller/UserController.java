package com.ecommerce.user.controller;

import com.ecommerce.user.dto.AddressDto;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UserDto;
import com.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
     * GET /v1/users/{userId}/addresses
     * Get all addresses for a user. Requires JWT (self).
     */
    @GetMapping("/{userId}/addresses")
    public ResponseEntity<List<AddressDto>> getAddresses(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getAddresses(userId));
    }
}
