package com.paymentgateway.config;

import com.paymentgateway.model.entity.MerchantCredentials;
import com.paymentgateway.repository.MerchantCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds dummy merchant credentials on startup for testing.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  TEST CREDENTIALS  (use these in Postman / Swagger)                     │
 * │                                                                         │
 * │  Merchant 1 — Ecommerce Platform (default test merchant)                │
 * │    X-Api-Key    : pgw_test_ecomm_abc123xyz789                           │
 * │    X-Api-Secret : ecomm_secret_do_not_use_in_prod                       │
 * │                                                                         │
 * │  Merchant 2 — Flipkart Demo                                             │
 * │    X-Api-Key    : pgw_test_flipkart_def456uvw012                        │
 * │    X-Api-Secret : flipkart_secret_do_not_use_in_prod                    │
 * │                                                                         │
 * │  NOTE: These are seeded with connectTimeoutMs=5000, readTimeoutMs=30000 │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantCredentialsSeeder implements CommandLineRunner {

    private static final String MERCHANT_1_ID     = "MERCH_ECOMM_001";
    private static final String MERCHANT_1_KEY    = "pgw_test_ecomm_abc123xyz789";
    private static final String MERCHANT_1_SECRET = "ecomm_secret_do_not_use_in_prod";

    private static final String MERCHANT_2_ID     = "MERCH_FLIPKART_002";
    private static final String MERCHANT_2_KEY    = "pgw_test_flipkart_def456uvw012";
    private static final String MERCHANT_2_SECRET = "flipkart_secret_do_not_use_in_prod";

    private final MerchantCredentialsRepository repository;
    private final PasswordEncoder               passwordEncoder;

    @Override
    public void run(String... args) {
        seedIfAbsent(MERCHANT_1_ID, "Ecommerce Platform",
                MERCHANT_1_KEY, MERCHANT_1_SECRET, 5000, 30000);

        seedIfAbsent(MERCHANT_2_ID, "Flipkart Demo",
                MERCHANT_2_KEY, MERCHANT_2_SECRET, 3000, 20000);

        log.info("""
                \n
                ╔══════════════════════════════════════════════════════════════════════╗
                ║             PAYMENT GATEWAY — TEST CREDENTIALS READY                ║
                ╠══════════════════════════════════════════════════════════════════════╣
                ║  Merchant 1 — Ecommerce Platform                                    ║
                ║    X-Api-Key    : pgw_test_ecomm_abc123xyz789                       ║
                ║    X-Api-Secret : ecomm_secret_do_not_use_in_prod                   ║
                ║    Timeouts     : connect=5000ms  read=30000ms                      ║
                ╠══════════════════════════════════════════════════════════════════════╣
                ║  Merchant 2 — Flipkart Demo                                         ║
                ║    X-Api-Key    : pgw_test_flipkart_def456uvw012                    ║
                ║    X-Api-Secret : flipkart_secret_do_not_use_in_prod                ║
                ║    Timeouts     : connect=3000ms  read=20000ms                      ║
                ╠══════════════════════════════════════════════════════════════════════╣
                ║  Swagger UI : http://localhost:8080/swagger-ui.html                 ║
                ║  Add headers: X-Api-Key and X-Api-Secret in Swagger Authorize       ║
                ╚══════════════════════════════════════════════════════════════════════╝
                """);
    }

    private void seedIfAbsent(String merchantId, String merchantName,
                               String apiKey, String rawSecret,
                               int connectTimeoutMs, int readTimeoutMs) {
        if (repository.findByApiKey(apiKey).isEmpty()) {
            MerchantCredentials creds = MerchantCredentials.builder()
                    .merchantId(merchantId)
                    .merchantName(merchantName)
                    .apiKey(apiKey)
                    .apiSecretHash(passwordEncoder.encode(rawSecret)) // BCrypt hash
                    .connectTimeoutMs(connectTimeoutMs)
                    .readTimeoutMs(readTimeoutMs)
                    .active(true)
                    .environment("TEST")
                    .build();
            repository.save(creds);
            log.info("Seeded credentials for merchant: {} ({})", merchantName, apiKey);
        } else {
            log.debug("Credentials already exist for apiKey: {}", apiKey);
        }
    }
}
