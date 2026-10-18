-- Add a durable execution state before contacting Delhivery.
-- Existing rows and booking attempts are preserved.

ALTER TABLE delhivery_shipment_booking_attempts
    DROP CONSTRAINT IF EXISTS chk_delhivery_booking_status;

ALTER TABLE delhivery_shipment_booking_attempts
    ADD CONSTRAINT chk_delhivery_booking_status
    CHECK (
        attempt_status IN (
            'PENDING',
            'PROCESSING',
            'SUCCEEDED',
            'UNKNOWN',
            'FAILED'
        )
    );