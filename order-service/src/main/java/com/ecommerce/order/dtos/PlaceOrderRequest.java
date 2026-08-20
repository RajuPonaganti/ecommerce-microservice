package com.ecommerce.order.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Incoming request body for POST /orders. */
public record PlaceOrderRequest(@NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotNull Long productId,
            @NotNull @Positive Integer quantity) {
    }
}
