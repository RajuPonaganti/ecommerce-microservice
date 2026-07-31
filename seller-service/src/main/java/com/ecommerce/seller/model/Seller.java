package com.ecommerce.seller.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seller")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Seller {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID sellerId;

	@Column(nullable = false, length = 250) 
	private String legalName;
	
	@Column(nullable = false, length = 100) 
	private String tradeName;

	@Column(nullable = false, length = 20) 
	private String gstin;
	
	@Column(nullable = false, length = 10) 
	private String pan;
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "seller", cascade = CascadeType.ALL)
	private	SellerBankAccount bankAccount;
	
	@Enumerated(EnumType.STRING)
	private	SellerStatus sellerStatus;
	
	@Enumerated(EnumType.STRING)
	private	CommissionTier commissionTier;

}

