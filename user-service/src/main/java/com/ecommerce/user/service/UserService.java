package com.ecommerce.user.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.user.dto.AddressDto;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UserDto;
import com.ecommerce.user.exception.ConflictException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.model.User;
import com.ecommerce.user.model.UserAddress;
import com.ecommerce.user.model.UserCredentials;
import com.ecommerce.user.model.UserStatus;
import com.ecommerce.user.repository.AddressRepository;
import com.ecommerce.user.repository.UserCredentialsRepository;
import com.ecommerce.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserCredentialsRepository credentialsRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Register ─────────────────────────────────────────────────────────────

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }
        if (userRepository.existsByPhoneNumber(request.phone())) {
            throw new ConflictException("Phone number already in use: " + request.phone());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPhoneNumber(request.phone());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setMfaEnabled(false);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);

        UserCredentials credentials = new UserCredentials();
        credentials.setPassword(passwordEncoder.encode(request.password()));
        credentials.setCreatedAt(Instant.now());
        credentials.setUser(user);
        credentialsRepository.save(credentials);

        return toDto(user);
    }

    // ── Get user ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDto getUser(UUID userId) {
        return toDto(findUser(userId));
    }

    // ── Update user ───────────────────────────────────────────────────────────

    @Transactional
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        User user = findUser(userId);

        if (request.phone() != null && !request.phone().isBlank()) {
            if (!request.phone().equals(user.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(request.phone())) {
                throw new ConflictException("Phone number already in use: " + request.phone());
            }
            user.setPhoneNumber(request.phone());
        }

        user.setUpdatedAt(Instant.now());
        return toDto(userRepository.save(user));
    }

    // ── Soft delete ───────────────────────────────────────────────────────────

    @Transactional
    public void deleteUser(UUID userId) {
        User user = findUser(userId);
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    // ── Addresses ─────────────────────────────────────────────────────────────

    @Transactional
    public AddressDto addAddress(UUID userId, AddressDto dto) {
        User user = findUser(userId);

        UserAddress address = new UserAddress();
        address.setLabel(dto.label());
        address.setStreet(dto.street());
        address.setCity(dto.city());
        address.setPincode(dto.pincode());
        address.setDefault(dto.isDefault());
        address.setCreatedAt(Instant.now());
        address.setUser(user);

        return toAddressDto(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressDto> getAddresses(UUID userId) {
        findUser(userId); // ensure user exists
        return addressRepository.findByUser_UserId(userId)
                .stream()
                .map(this::toAddressDto)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getUserId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.isMfaEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private AddressDto toAddressDto(UserAddress address) {
        return new AddressDto(
                address.getAddressId(),
                address.getLabel(),
                address.getStreet(),
                address.getCity(),
                address.getPincode(),
                address.isDefault()
        );
    }
}
