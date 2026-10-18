package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import in.kcmart.backend.shipping.delhivery.service.DelhiveryPackageService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDelhiveryPackageControllerTest {

    private final DelhiveryPackageService service =
        mock(DelhiveryPackageService.class);

    private final AdminDelhiveryPackageController controller =
        new AdminDelhiveryPackageController(service);

    private DelhiveryShipmentPackage packageDetails() {
        return new DelhiveryShipmentPackage(
            5L,
            1200,
            new BigDecimal("30"),
            new BigDecimal("20"),
            new BigDecimal("15"),
            1800,
            1800
        );
    }

    @Test
    void readsPackageThroughService() {
        when(service.getByOrderId(5L))
            .thenReturn(packageDetails());

        var response = controller.get(5L);

        assertEquals(5L, response.orderId());
        assertEquals(1200, response.actualWeightGrams());
        assertEquals(1800, response.chargeableWeightGrams());

        verify(service).getByOrderId(5L);
    }

    @Test
    void createsPackageThroughService() {
        when(service.create(
            eq(5L),
            eq(1200),
            eq(new BigDecimal("30")),
            eq(new BigDecimal("20")),
            eq(new BigDecimal("15"))
        )).thenReturn(packageDetails());

        var request =
            new AdminDelhiveryPackageController.PackageRequest(
                1200,
                new BigDecimal("30"),
                new BigDecimal("20"),
                new BigDecimal("15")
            );

        var response = controller.create(5L, request);

        assertEquals(1800, response.volumetricWeightGrams());
        assertEquals(1800, response.chargeableWeightGrams());

        verify(service).create(
            eq(5L),
            eq(1200),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class)
        );
    }

    @Test
    void propagatesMissingPackageError() {
        when(service.getByOrderId(5L))
            .thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Package details not found."
            ));

        assertThrows(ResponseStatusException.class, () ->
            controller.get(5L)
        );
    }
}