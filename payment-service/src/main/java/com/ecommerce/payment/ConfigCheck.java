package com.ecommerce.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class ConfigCheck implements CommandLineRunner {
    @Value("${payment.gateway.base-url:NOT_FOUND}")
    private String baseUrl;

    @Override
    public void run(String... args) {
        System.out.println(">>> payment-gateway.base-url = " + baseUrl);
    }
}
