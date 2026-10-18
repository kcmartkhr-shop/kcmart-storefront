package in.kcmart.backend.shipping.delhivery.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "delhivery_shipment_packages")
public class DelhiveryShipmentPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "actual_weight_grams", nullable = false)
    private int actualWeightGrams;

    @Column(name = "length_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "volumetric_weight_grams", nullable = false)
    private int volumetricWeightGrams;

    @Column(name = "chargeable_weight_grams", nullable = false)
    private int chargeableWeightGrams;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected DelhiveryShipmentPackage() {}

    public DelhiveryShipmentPackage(
            Long orderId,
            int actualWeightGrams,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            int volumetricWeightGrams,
            int chargeableWeightGrams) {

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Valid order ID required.");
        }

        if (actualWeightGrams <= 0
                || volumetricWeightGrams <= 0
                || chargeableWeightGrams < actualWeightGrams
                || chargeableWeightGrams < volumetricWeightGrams) {
            throw new IllegalArgumentException("Invalid package weights.");
        }

        if (lengthCm == null || widthCm == null || heightCm == null
                || lengthCm.signum() <= 0
                || widthCm.signum() <= 0
                || heightCm.signum() <= 0) {
            throw new IllegalArgumentException("Invalid package dimensions.");
        }

        this.orderId = orderId;
        this.actualWeightGrams = actualWeightGrams;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.volumetricWeightGrams = volumetricWeightGrams;
        this.chargeableWeightGrams = chargeableWeightGrams;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public int getActualWeightGrams() { return actualWeightGrams; }
    public BigDecimal getLengthCm() { return lengthCm; }
    public BigDecimal getWidthCm() { return widthCm; }
    public BigDecimal getHeightCm() { return heightCm; }
    public int getVolumetricWeightGrams() { return volumetricWeightGrams; }
    public int getChargeableWeightGrams() { return chargeableWeightGrams; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}