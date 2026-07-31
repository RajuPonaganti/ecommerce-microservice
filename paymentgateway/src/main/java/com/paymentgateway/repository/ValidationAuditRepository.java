package com.paymentgateway.repository;

import com.paymentgateway.model.entity.ValidationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValidationAuditRepository extends JpaRepository<ValidationAudit, Long> {

    List<ValidationAudit> findByTransactionIdOrderByCreatedAt(String transactionId);

    List<ValidationAudit> findByTransactionIdAndResult(String transactionId, String result);

    List<ValidationAudit> findByRuleName(String ruleName);
}
