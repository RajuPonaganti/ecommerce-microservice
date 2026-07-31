package com.paymentgateway.repository;

import com.paymentgateway.model.entity.Transaction;
import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByOrderId(String orderId);

    List<Transaction> findByMerchantId(String merchantId);

    List<Transaction> findByStatus(PaymentStatus status);

    List<Transaction> findByPaymentMode(PaymentMode paymentMode);

    List<Transaction> findByCustomerEmail(String customerEmail);

    boolean existsByOrderId(String orderId);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    List<Transaction> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
