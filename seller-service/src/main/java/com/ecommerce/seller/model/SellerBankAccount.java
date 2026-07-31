package com.ecommerce.seller.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seller_bank_account")
@Setter
@Getter
@Builder
public class SellerBankAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID bankAccountId;

	@Column(nullable = false, length = 15) 
	private String IFSC;
	
	@Column(nullable = false) 
	private Long acccountNumber;
	
	@Column(nullable = false, length = 100) 
	private String accountHolderName;

	@Column(nullable = false, length = 20) 
	private String accountType;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_id")
	private Seller seller;
}
