package com.paymentgateway.repository;

import com.paymentgateway.model.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundId(String refundId);

    List<Refund> findByTransaction_TransactionId(String transactionId);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM   Refund r
            WHERE  r.transaction.transactionId = :transactionId
              AND  r.status = 'SUCCESS'
            """)
    BigDecimal getTotalRefundedAmount(@Param("transactionId") String transactionId);
}
