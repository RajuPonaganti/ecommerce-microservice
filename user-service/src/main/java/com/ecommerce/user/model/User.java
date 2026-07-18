package com.ecommerce.user.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
@Setter
@Getter
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID userId;

	// @Column specifies the database column properties
	@Column(nullable = false, length = 320) // nullable=false means NOT NULL in SQL
	private String email;

	// @Column specifies the database column properties
	@Column(nullable = false, length = 20)
	private String phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status = UserStatus.PENDING_VERIFICATION;

	@Column(nullable = false)
	private boolean mfaEnabled;
	

	@Column(nullable = false)
	private Instant createdAt;

	private Instant updatedAt;

	private Instant deletedAt;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user")
	private List<UserAddress> address;

}
