package in.kcmart.backend.order.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderInvoice;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class OrderInvoiceEmailSender {

    private final JavaMailSender mailSender;
    private final InvoiceStorageService storage;
    private final String fromAddress;

    public OrderInvoiceEmailSender(
            JavaMailSender mailSender,
            InvoiceStorageService storage,
            @Value("${kcmart.mail.from}") String fromAddress) {

        this.mailSender = mailSender;
        this.storage = storage;
        this.fromAddress = fromAddress;
    }

    public void send(Order order, OrderInvoice invoice)
            throws MessagingException {

        Resource invoiceFile = storage.load(
            invoice.getStorageKey()
        );

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(
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
            "Your KCMART Invoice - " + order.getOrderNumber()
        );

        helper.setText(buildHtml(order, invoice), true);

        helper.addAttachment(
            invoice.getOriginalFilename(),
            invoiceFile,
            "application/pdf"
        );

        mailSender.send(message);
    }

    private String buildHtml(Order order, OrderInvoice invoice) {

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:24px;background:#f3f5f7;font-family:Arial,sans-serif;color:#17212b;">
              <div style="max-width:620px;margin:auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;">
                <div style="background:#075e54;color:#ffffff;padding:26px;">
                  <h1 style="margin:0;font-size:26px;">KCMART</h1>
                  <p style="margin:8px 0 0;">Khariar Computer Service</p>
                </div>
                <div style="padding:28px;">
                  <h2 style="margin-top:0;">Your Invoice Is Ready</h2>
                  <p>Hello %s,</p>
                  <p>Thank you for shopping with KCMART. Please find your invoice attached as a PDF.</p>
                  <div style="background:#eaf5f2;padding:18px;border-radius:10px;">
                    <p><strong>Order Number:</strong> %s</p>
                    <p><strong>Invoice Number:</strong> %s</p>
                    <p><strong>Total Amount:</strong> &#8377;%s</p>
                  </div>
                  <p>Please retain this invoice for your records.</p>
                  <hr style="border:0;border-top:1px solid #e5e7eb;margin:26px 0;">
                  <strong>Khariar Computer Service</strong>
                  <p style="line-height:1.6;">
                    College Road, Near Koshaleswar Temple<br>
                    Raj Khariar, Odisha - 766107<br>
                    Mobile: 8637277240
                  </p>
                  <p style="color:#6b7280;font-size:12px;">This is an automated invoice email from KCMART.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                escapeHtml(order.getCustomerName()),
                escapeHtml(order.getOrderNumber()),
                escapeHtml(invoice.getInvoiceNumber()),
                order.getTotalAmount().toPlainString()
            );
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