package com.paymentgateway.repository;

import com.paymentgateway.model.entity.UpiPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UpiPaymentDetailsRepository extends JpaRepository<UpiPaymentDetails, Long> {

    Optional<UpiPaymentDetails> findByTransaction_TransactionId(String transactionId);
}
