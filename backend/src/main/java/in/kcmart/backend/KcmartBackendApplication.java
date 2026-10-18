package in.kcmart.backend;

import in.kcmart.backend.payment.razorpay.config.RazorpayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(RazorpayProperties.class)
@EnableScheduling
public class KcmartBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KcmartBackendApplication.class, args);
    }
}