package com.ecommerce.user.repository;

import com.ecommerce.user.model.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {

	Optional<UserCredentials> findByUser_UserId(UUID userId);
}
