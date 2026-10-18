package in.kcmart.backend;

import in.kcmart.backend.order.reservation.service.DeliveryReservationExpiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class KcmartBackendApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
    }

    @Test
    void schedulerDisabledInTestProfile() {
        String[] beans = applicationContext.getBeanNamesForType(
                DeliveryReservationExpiryService.class
        );

        assertTrue(
                beans.length == 0,
                "Reservation expiry scheduler must be disabled in the test profile"
        );
    }
}