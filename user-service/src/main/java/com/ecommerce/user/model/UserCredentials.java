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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_credentials")
@Setter
@Getter
public class UserCredentials {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID userCredentialId;
	
	@Column(nullable = false, length = 500) // nullable=false means NOT NULL in SQL
	private String password;
	
	@Column(nullable = false)
	private Instant createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;
}
