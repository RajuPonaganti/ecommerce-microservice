package com.ecommerce.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.model.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	@Modifying
	@Query("UPDATE Order o SET o.status = :status WHERE o.orderId = :orderId")
	void updateStatus(@Param("orderId") UUID orderId, @Param("status") OrderStatus status);
}
