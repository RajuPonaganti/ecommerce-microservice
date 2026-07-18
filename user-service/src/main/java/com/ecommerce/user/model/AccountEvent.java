package com.ecommerce.user.model;

import java.time.Instant;
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
@Table(name = "account_event")
@Setter
@Getter
//(event store for FSM replay)
public class AccountEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID accountEventId;
	
	@Enumerated(EnumType.STRING)
	private EventType eventType;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "userid")
	private User user;
	
	private String actor;
	
	private Instant occurredAt;
}
