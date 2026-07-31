package com.paymentgateway.repository;

import com.paymentgateway.model.entity.NetBankingPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetBankingPaymentDetailsRepository extends JpaRepository<NetBankingPaymentDetails, Long> {

    Optional<NetBankingPaymentDetails> findByTransaction_TransactionId(String transactionId);
}
