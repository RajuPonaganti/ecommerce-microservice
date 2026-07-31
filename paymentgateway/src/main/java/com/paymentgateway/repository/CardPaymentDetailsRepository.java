package com.paymentgateway.repository;

import com.paymentgateway.model.entity.CardPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardPaymentDetailsRepository extends JpaRepository<CardPaymentDetails, Long> {

    Optional<CardPaymentDetails> findByTransaction_TransactionId(String transactionId);
}
