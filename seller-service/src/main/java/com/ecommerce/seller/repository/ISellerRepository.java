package com.ecommerce.seller.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.seller.model.Seller;

public interface ISellerRepository extends JpaRepository<Seller, UUID>{

}
