package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryPackageConfiguration;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class DelhiveryPackageService {

    private final OrderRepository orders;
    private final OrderShipmentRepository shipments;
    private final DelhiveryShipmentPackageRepository packages;
    private final DelhiveryShipmentBookingAttemptRepository attempts;
    private final DelhiveryPackageWeightCalculator calculator;
    private final DelhiveryPackageConfiguration packageConfiguration;

    @Autowired
    public DelhiveryPackageService(
            OrderRepository orders,
            OrderShipmentRepository shipments,
            DelhiveryShipmentPackageRepository packages,
            DelhiveryShipmentBookingAttemptRepository attempts,
            DelhiveryPackageWeightCalculator calculator,
            DelhiveryPackageConfiguration packageConfiguration) {

        this.orders = orders;
        this.shipments = shipments;
        this.packages = packages;
        this.attempts = attempts;
        this.calculator = calculator;
        this.packageConfiguration = packageConfiguration;
    }

    public DelhiveryPackageService(
            OrderRepository orders,
            OrderShipmentRepository shipments,
            DelhiveryShipmentPackageRepository packages,
            DelhiveryShipmentBookingAttemptRepository attempts,
            DelhiveryPackageWeightCalculator calculator) {

        this(
            orders,
            shipments,
            packages,
            attempts,
            calculator,
            new DelhiveryPackageConfiguration(new BigDecimal("5000"))
        );
    }

    @Transactional(readOnly = true)
    public DelhiveryShipmentPackage getByOrderId(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Valid order ID required."
            );
        }

        return packages.findByOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Package details not found."
            ));
    }

    @Transactional
    public DelhiveryShipmentPackage create(
            Long orderId,
            int actualWeightGrams,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm) {

        return create(
            orderId,
            actualWeightGrams,
            lengthCm,
            widthCm,
            heightCm,
            packageConfiguration.getVolumetricDivisor()
        );
    }
    @Transactional
    public DelhiveryShipmentPackage create(
            Long orderId,
            int actualWeightGrams,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            BigDecimal volumetricDivisor) {

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Valid order ID required."
            );
        }

        Order order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        if (!"DELIVERY".equals(normalize(order.getFulfillmentType()))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Package details are only for DELIVERY orders."
            );
        }

        if (!"PACKED".equals(normalize(order.getOrderStatus()))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order must be PACKED before package preparation."
            );
        }

        if (shipments.findByOrderIdForUpdate(orderId)
                .filter(shipment ->
                    !"PACKED".equals(shipment.getShipmentStatus() == null ? "" : shipment.getShipmentStatus().trim().toUpperCase(java.util.Locale.ROOT))
                    || (shipment.getCarrierName() != null
                        && !shipment.getCarrierName().isBlank())
                    || (shipment.getTrackingNumber() != null
                        && !shipment.getTrackingNumber().isBlank())
                )
                .isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Shipment already exists."
            );
        }

        if (attempts.existsByOrderId(orderId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Booking attempt exists. Package changes blocked."
            );
        }

        if (packages.existsByOrderId(orderId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Package already saved. Duplicate creation blocked."
            );
        }

        DelhiveryPackageWeightCalculator.WeightResult weight;

        try {
            weight = calculator.calculate(
                actualWeightGrams,
                lengthCm,
                widthCm,
                heightCm,
                volumetricDivisor
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
            );
        }

        DelhiveryShipmentPackage shipmentPackage =
            new DelhiveryShipmentPackage(
                orderId,
                weight.actualWeightGrams(),
                weight.lengthCm(),
                weight.widthCm(),
                weight.heightCm(),
                weight.volumetricWeightGrams(),
                weight.chargeableWeightGrams()
            );

        return packages.saveAndFlush(shipmentPackage);
    }

    private String normalize(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT);
    }
}