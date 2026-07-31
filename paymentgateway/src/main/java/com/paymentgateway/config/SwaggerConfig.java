package com.paymentgateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger UI configuration.
 *
 * <p>Interactive UI: {@code http://localhost:8080/swagger-ui.html}<br>
 * Raw spec:          {@code http://localhost:8080/api-docs}</p>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI paymentGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Gateway Simulation API")
                        .description("""
                                A mock Payment Gateway that simulates real-world payment processing for testing.

                                ## Payment Modes
                                | Mode | Verify Step | Detail Entity |
                                |---|---|---|
                                | **CARD** | Yes – OTP via `/verify` | `cardDetails` nested object |
                                | **NET_BANKING** | Yes – bank callback via `/verify` | `netBankingDetails` nested object |
                                | **UPI** | No – resolves immediately | `upiDetails` nested object |

                                ## Validation (per mode)
                                Every request goes through a dedicated `PaymentValidator` before processing.
                                All rule results (PASS/FAIL) are persisted to `validation_audit` and queryable
                                via `GET /api/v1/payments/{transactionId}/validation-audit`.

                                | Mode | Rules |
                                |---|---|
                                | CARD | Number present, 15–19 digits, **Luhn algorithm**, expiry format, expiry not past, CVV present, CVV format _(CVV never stored)_, holder name, amount positive |
                                | NET_BANKING | Bank code present, **exists in bank master**, **is active**, **supports net banking**, amount positive |
                                | UPI | VPA present, **format regex**, bank handle in master list, amount positive |

                                ## Simulation Behaviour
                                - **Card**: 75% success · 10% wrong-OTP · 15% insufficient balance
                                - **Net Banking**: 85% success · 5% bank timeout · 10% invalid credentials
                                - **UPI**: 80% success · 20% failure (ID not registered / bank declined)

                                ## Database Design
                                Mode-specific details are stored in **separate composition tables**
                                (`card_payment_details`, `upi_payment_details`, `netbanking_payment_details`)
                                linked back to `transactions` via FK on `transaction_id`.
                                The `transactions` table itself holds only common fields.
                                """)
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Payment Gateway Team")
                                .email("support@paymentgateway.com")
                                .url("https://paymentgateway.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("Payment Gateway Integration Guide")
                        .url("https://paymentgateway.com/docs"))
                .components(new Components());
    }
}
