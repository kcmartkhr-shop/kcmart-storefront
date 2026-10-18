package in.kcmart.backend.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InvoiceEmailSafetyGate {

    private final boolean mailEnabled;

    public InvoiceEmailSafetyGate(
            @Value("${KCMART_MAIL_ENABLED:false}") boolean mailEnabled) {
        this.mailEnabled = mailEnabled;
    }

    public void requireEnabled() {
        if (!mailEnabled) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Invoice email delivery is currently disabled."
            );
        }
    }

    public boolean isEnabled() {
        return mailEnabled;
    }
}