package com.ecommerce.seller.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seller_payout")
@Setter
@Getter
public class SellerPayout {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID payoutId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_id")
	private Seller seller;

	private LocalDate periodStart;
	
	private LocalDate periodEnd;
	
	private BigDecimal grossSales;
	private BigDecimal platformFee;
	private BigDecimal taxDeducted;
	private BigDecimal netAmount;
	
	@Enumerated(EnumType.STRING)
	private SellerPayoutStatus sellerPayoutStatus;
}
