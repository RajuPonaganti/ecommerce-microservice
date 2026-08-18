package com.paymentgateway.repository;

import com.paymentgateway.model.entity.MerchantCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantCredentialsRepository extends JpaRepository<MerchantCredentials, Long> {

    Optional<MerchantCredentials> findByApiKey(String apiKey);
}
