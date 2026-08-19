package com.ecommerce.notification.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Email dispatcher — simulated locally, real SendGrid call when enabled.
 * Replace the body of sendReal() with actual SendGrid SDK call in production.
 */
@Component
@Slf4j
public class EmailDispatcher {

    @Value("${notification.email.enabled:false}")
    private boolean enabled;

    @Value("${notification.email.from:ponaganti.raju466@gmail.com}")
    private String from;

    /**
     * @param to       recipient email address
     * @param subject  email subject
     * @param body     email body (plain text or HTML)
     */
    public void send(String to, String subject, String body) {
        if (!enabled) {
            // Simulation mode — log what would have been sent
            log.info("[EMAIL-SIMULATED] to={} | subject={} | body={}", to, subject, body);
            return;
        }
        // Production: replace with SendGrid SDK call
        // SendGrid sg = new SendGrid(apiKey);
        // Request request = new Request(); ... sg.api(request);
        log.info("[EMAIL-SENT] to={} | subject={}", to, subject);
    }
}
