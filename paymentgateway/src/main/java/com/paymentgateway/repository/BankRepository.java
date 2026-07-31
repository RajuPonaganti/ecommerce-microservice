package com.paymentgateway.repository;

import com.paymentgateway.model.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {

    Optional<Bank> findByBankCodeIgnoreCase(String bankCode);

    Optional<Bank> findByBankCodeIgnoreCaseAndIsActiveTrue(String bankCode);

    List<Bank> findByIsActiveTrueOrderByBankName();

    List<Bank> findBySupportsNetBankingTrueAndIsActiveTrueOrderByBankName();

    List<Bank> findBySupportsUpiTrueAndIsActiveTrueOrderByBankName();

    boolean existsByBankCodeIgnoreCase(String bankCode);
}
