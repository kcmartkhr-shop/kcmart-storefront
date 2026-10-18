package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DelhiveryBookingPreviewService {

    private final OrderRepository orders;
    private final OrderShipmentRepository shipments;
    private final DelhiveryShipmentPackageRepository packages;
    private final DelhiveryShipmentBookingAttemptRepository attempts;
    private final DelhiveryBookingEligibilityValidator validator;

    public DelhiveryBookingPreviewService(
            OrderRepository orders,
            OrderShipmentRepository shipments,
            DelhiveryShipmentPackageRepository packages,
            DelhiveryShipmentBookingAttemptRepository attempts,
            DelhiveryBookingEligibilityValidator validator) {

        this.orders = orders;
        this.shipments = shipments;
        this.packages = packages;
        this.attempts = attempts;
        this.validator = validator;
    }

    public record Preview(
        Long orderId,
        boolean locallyReady,
        boolean prepaidOnly,
        boolean serviceabilityVerified,
        boolean shipmentBooked,
        List<String> blockers
    ) {}

    @Transactional(readOnly = true)
    public Preview preview(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Valid order ID required."
            );
        }

        Order order = orders.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Order not found."
            ));

        List<String> blockers = new ArrayList<>();

        var shipmentPackage = packages.findByOrderId(orderId)
            .orElse(null);

        try {
            validator.validate(order, shipmentPackage);
        } catch (ResponseStatusException ex) {
            blockers.add("Order, payment, address or package eligibility failed.");
        }

        boolean shipmentBooked = false;

        var shipment = shipments.findByOrderId(orderId)
            .orElse(null);

        if (shipment != null) {
            shipmentBooked =
                hasValue(shipment.getTrackingNumber())
                || hasValue(shipment.getCarrierName());

            if (!"PACKED".equals(normalize(shipment.getShipmentStatus()))
                    || shipmentBooked) {
                blockers.add("Existing shipment is not available for booking.");
            }
        }

        if (attempts.existsByOrderId(orderId)) {
            blockers.add("A Delhivery booking attempt already exists.");
        }

        return new Preview(
            orderId,
            blockers.isEmpty(),
            true,
            false,
            shipmentBooked,
            List.copyOf(blockers)
        );
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT);
    }
}