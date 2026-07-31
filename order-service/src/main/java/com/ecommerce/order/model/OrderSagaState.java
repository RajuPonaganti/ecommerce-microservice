package com.ecommerce.order.model;

import java.time.Instant;
import java.util.UUID;

import com.ecommerce.order.enums.OrderState;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saga_state")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class OrderSagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
	
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private OrderState status;

    private String lastError;

    private int retryCount;

    @Version
    private Long version;              // optimistic locking — prevents 2 workers processing the same saga at once

    private Instant updatedAt;

}
