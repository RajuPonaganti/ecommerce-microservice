package com.ecommerce.seller.service;

import org.springframework.stereotype.Service;

import com.ecommerce.seller.dto.BankAccountDetailsDTO;
import com.ecommerce.seller.dto.SellerDTO;
import com.ecommerce.seller.model.CommissionTier;
import com.ecommerce.seller.model.Seller;
import com.ecommerce.seller.model.SellerBankAccount;
import com.ecommerce.seller.model.SellerStatus;
import com.ecommerce.seller.repository.ISellerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {

	private final ISellerRepository sellerRepository;

	public SellerDTO saveSeller(final SellerDTO dto) {
		log.info("SellerService.saveSeller() | legalName={} | gstin={}", dto.getLegalName(), dto.getGstin());

		BankAccountDetailsDTO baDetails = dto.getBaDetails();

		final Seller seller = Seller.builder()
				.legalName(dto.getLegalName())
				.tradeName(dto.getTradeName())
				.gstin(dto.getGstin())
				.pan(dto.getPan())
				.sellerStatus(SellerStatus.PENDING_KYC)
				.commissionTier(CommissionTier.STANDARD)
				.build();

		SellerBankAccount bankAccount = SellerBankAccount.builder()
				.acccountNumber(baDetails.getAccountNumber())
				.accountHolderName(baDetails.getAccountHolderName())
				.IFSC(baDetails.getIFSC())
				.accountType(baDetails.getAccountType())
				.seller(seller)
				.build();

		seller.setBankAccount(bankAccount);

		Seller save = sellerRepository.save(seller);
		dto.setSellerId(save.getSellerId());

		log.info("SellerService.saveSeller() | seller created | sellerId={} | status=PENDING_KYC", save.getSellerId());
		return dto;
	}
}
