package com.ecommerce.order.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.order.enums.OrderState;
import com.ecommerce.order.model.OrderSagaState;

public interface OrderSagaStateRepository extends JpaRepository<OrderSagaState, UUID> {
	 List<OrderSagaState> findByStatusInAndUpdatedAtBefore(
	            Collection<OrderState> statuses, Instant cutoff);
}
