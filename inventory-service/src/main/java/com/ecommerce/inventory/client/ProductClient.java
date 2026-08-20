package com.ecommerce.inventory.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecommerce.inventory.dtos.ProductResponse;

/**
 * Declarative HTTP client for product-catalog-service.
 *
 * "name" is resolved through Eureka - Spring Cloud LoadBalancer picks a
 * live product-catalog-service instance at call time, so no host/port is
 * ever hardcoded here.
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/v1/products/order/{productId}")
    ProductResponse getProductForOrder(@PathVariable("productId") UUID productId);
}
