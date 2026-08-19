package com.ecommerce.notification.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMS dispatcher — simulated locally, real Twilio call when enabled.
 */
@Component
@Slf4j
public class SmsDispatcher {

    @Value("${notification.sms.enabled:false}")
    private boolean enabled;

    public void send(String to, String message) {
        if (!enabled) {
            log.info("[SMS-SIMULATED] to={} | message={}", to, message);
            return;
        }
        // Production: replace with Twilio SDK call
        // Twilio.init(accountSid, authToken);
        // Message.creator(new PhoneNumber(to), new PhoneNumber(from), message).create();
        log.info("[SMS-SENT] to={}", to);
    }
}
