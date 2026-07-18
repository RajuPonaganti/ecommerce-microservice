package com.ecommerce.user.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_address")
@Setter
@Getter
public class UserAddress {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID addressId;

	private String label;
	@Lob
	@Column(nullable = false)
	private String street;
	
	@Column(nullable = false)
	private String city;
	
	@Column(nullable = false)
	private Integer pincode;
	
	@Column(nullable = false)
	private Instant createdAt;

	private Instant updatedAt;
	
	@Column(nullable = false)
	private boolean isDefault;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;
}
