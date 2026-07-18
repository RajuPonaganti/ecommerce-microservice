package com.ecommerce.user.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.user.model.UserAddress;

@Repository
public interface AddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findByUser_UserId(UUID userId);
}
