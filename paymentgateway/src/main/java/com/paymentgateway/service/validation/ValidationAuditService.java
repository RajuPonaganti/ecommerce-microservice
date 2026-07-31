package com.paymentgateway.service.validation;

import com.paymentgateway.model.entity.ValidationAudit;
import com.paymentgateway.repository.ValidationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists {@link ValidationAudit} records produced by {@link PaymentValidator} implementations.
 *
 * <p>Uses {@code REQUIRES_NEW} so that audit rows are always committed independently
 * of the calling transaction — even if the outer transaction rolls back, the audit
 * trail of what was validated (and why something was rejected) is preserved.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationAuditService {

    private final ValidationAuditRepository validationAuditRepository;

    /**
     * Saves all validation results as audit records in a dedicated transaction.
     *
     * @param results     list of rule results from the validator
     * @param transactionId the transaction ID (may be a temporary "PRE-xxx" key
     *                      before the real transaction is persisted)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAll(List<PaymentValidator.ValidationResult> results, String transactionId) {
        if (results == null || results.isEmpty()) return;

        List<ValidationAudit> audits = results.stream()
                .map(r -> ValidationAudit.builder()
                        .transactionId(transactionId)
                        .ruleName(r.ruleName())
                        .result(r.result())
                        .message(r.message())
                        .paymentMode(r.paymentMode())
                        .build())
                .toList();

        validationAuditRepository.saveAll(audits);
        log.debug("Saved {} validation audit record(s) | txnId={}", audits.size(), transactionId);
    }

    /**
     * Returns all audit records for a transaction in chronological order.
     */
    @Transactional(readOnly = true)
    public List<ValidationAudit> getAuditTrail(String transactionId) {
        return validationAuditRepository.findByTransactionIdOrderByCreatedAt(transactionId);
    }
}
