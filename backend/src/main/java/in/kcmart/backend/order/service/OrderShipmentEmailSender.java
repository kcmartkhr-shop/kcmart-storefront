package in.kcmart.backend.order.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderInvoice;
import in.kcmart.backend.order.entity.OrderShipment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class OrderShipmentEmailSender {

    private final JavaMailSender mailSender;
    private final InvoiceStorageService storage;
    private final String fromAddress;

    public OrderShipmentEmailSender(
            JavaMailSender mailSender,
            InvoiceStorageService storage,
            @Value("${kcmart.mail.from}") String fromAddress) {

        this.mailSender = mailSender;
        this.storage = storage;
        this.fromAddress = fromAddress;
    }

    public void send(
            Order order,
            OrderShipment shipment,
            OrderInvoice invoice)
            throws MessagingException {

        Resource invoiceFile =
            storage.load(invoice.getStorageKey());

        MimeMessage message =
            mailSender.createMimeMessage();

        MimeMessageHelper helper =
            new MimeMessageHelper(
                message,
                true,
                StandardCharsets.UTF_8.name()
            );

        try {
            helper.setFrom(
                fromAddress,
                "Khariar Computer Service"
            );
        }
        catch (java.io.UnsupportedEncodingException ex) {
            throw new MessagingException(
                "Could not encode KCMART sender name.",
                ex
            );
        }

        helper.setTo(order.getCustomerEmail());

        helper.setSubject(
            "Your KCMART order has been shipped - "
                + order.getOrderNumber()
        );

        helper.setText(
            buildHtml(order, shipment, invoice),
            true
        );

        helper.addAttachment(
            invoice.getOriginalFilename(),
            invoiceFile,
            "application/pdf"
        );

        mailSender.send(message);
    }

    private String buildHtml(
            Order order,
            OrderShipment shipment,
            OrderInvoice invoice) {

        String trackingUrl =
            safeTrackingUrl(shipment.getTrackingUrl());

        String trackingSection = "";

        if (!trackingUrl.isBlank()) {
            trackingSection = """
                <p style="margin-top:20px;">
                    <a href="%s"
                       style="
                           display:inline-block;
                           background:#075E54;
                           color:#ffffff;
                           text-decoration:none;
                           padding:12px 18px;
                           border-radius:8px;
                           font-weight:bold;
                       ">
                        Track Your Order
                    </a>
                </p>
                """.formatted(
                    escapeHtml(trackingUrl)
                );
        }

        return """
            <!DOCTYPE html>
            <html>
            <body style="
                margin:0;
                padding:0;
                background:#f5f5f5;
                font-family:Arial,sans-serif;
                color:#111827;
            ">

            <div style="
                max-width:620px;
                margin:30px auto;
                background:#ffffff;
                border-radius:12px;
                overflow:hidden;
                border:1px solid #e5e7eb;
            ">

                <div style="
                    background:#075E54;
                    color:#ffffff;
                    padding:24px;
                ">
                    <h1 style="margin:0;font-size:24px;">
                        KCMART
                    </h1>

                    <p style="margin:8px 0 0;">
                        Khariar Computer Service
                    </p>
                </div>

                <div style="padding:28px;">

                    <h2 style="margin-top:0;">
                        Your order is on the way!
                    </h2>

                    <p>
                        Hello %s,
                    </p>

                    <p>
                        Your KCMART order has been shipped.
                        Use the tracking information below
                        to follow your delivery.
                    </p>

                    <div style="
                        background:#EAF5F2;
                        padding:18px;
                        border-radius:10px;
                        margin:22px 0;
                    ">

                        <p>
                            <strong>Order Number:</strong>
                            %s
                        </p>

                        <p>
                            <strong>Courier / Carrier:</strong>
                            %s
                        </p>

                        <p>
                            <strong>Tracking Number:</strong>
                            %s
                        </p>

                        <p>
                            <strong>Invoice Number:</strong>
                            %s
                        </p>

                    </div>

                    %s

                    <p style="margin-top:22px;">
                        Your invoice is attached to this email
                        as a PDF for your records.
                    </p>

                    <hr style="
                        border:none;
                        border-top:1px solid #e5e7eb;
                        margin:26px 0;
                    ">

                    <strong>Khariar Computer Service</strong>

                    <p style="line-height:1.6;">
                        College Road, Near Koshaleswar Temple<br>
                        Raj Khariar, Gadramunda<br>
                        Odisha - 766107<br>
                        Mobile: 8637277240<br>
                        GSTIN: 21AMZPK0583F1ZX
                    </p>

                    <p style="
                        color:#6B7280;
                        font-size:13px;
                        margin-bottom:0;
                    ">
                        This is an automated shipping notification from KCMART.
                    </p>

                </div>
            </div>

            </body>
            </html>
            """.formatted(
                escapeHtml(order.getCustomerName()),
                escapeHtml(order.getOrderNumber()),
                escapeHtml(shipment.getCarrierName()),
                escapeHtml(shipment.getTrackingNumber()),
                escapeHtml(invoice.getInvoiceNumber()),
                trackingSection
            );
    }

    private String safeTrackingUrl(String value) {

        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();

        if (trimmed.startsWith("https://")
                || trimmed.startsWith("http://")) {
            return trimmed;
        }

        return "";
    }

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}