package in.kcmart.backend.shipping.delhivery.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryShipmentBookingAttemptTest {

    private DelhiveryShipmentBookingAttempt processingAttempt() {
        var attempt = new DelhiveryShipmentBookingAttempt(123L);

        try {
            Field field = DelhiveryShipmentBookingAttempt.class
                .getDeclaredField("attemptStatus");
            field.setAccessible(true);
            field.set(attempt, "PROCESSING");
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }

        return attempt;
    }

    @Test
    void startsPending() {
        var attempt = new DelhiveryShipmentBookingAttempt(123L);

        assertEquals("PENDING", attempt.getAttemptStatus());
        assertEquals(123L, attempt.getOrderId());
    }

    @Test
    void pendingCannotDirectlySucceed() {
        var attempt = new DelhiveryShipmentBookingAttempt(123L);

        assertThrows(
            IllegalStateException.class,
            () -> attempt.markSucceeded("AWB123456")
        );

        assertEquals("PENDING", attempt.getAttemptStatus());
    }

    @Test
    void processingCanSucceed() {
        var attempt = processingAttempt();

        attempt.markSucceeded("AWB123456");

        assertEquals("SUCCEEDED", attempt.getAttemptStatus());
        assertEquals("AWB123456", attempt.getWaybill());
    }

    @Test
    void succeededCannotBecomeUnknown() {
        var attempt = processingAttempt();
        attempt.markSucceeded("AWB123456");

        assertThrows(
            IllegalStateException.class,
            () -> attempt.markUnknown("Timeout")
        );

        assertEquals("SUCCEEDED", attempt.getAttemptStatus());
    }

    @Test
    void succeededCannotBecomeFailed() {
        var attempt = processingAttempt();
        attempt.markSucceeded("AWB123456");

        assertThrows(
            IllegalStateException.class,
            () -> attempt.markFailed("Failure")
        );

        assertEquals("SUCCEEDED", attempt.getAttemptStatus());
    }

    @Test
    void processingCanBecomeUnknown() {
        var attempt = processingAttempt();

        attempt.markUnknown("Response timeout");

        assertEquals("UNKNOWN", attempt.getAttemptStatus());
    }

    @Test
    void unknownCannotAutomaticallyBecomeSucceeded() {
        var attempt = processingAttempt();
        attempt.markUnknown("Response timeout");

        assertThrows(
            IllegalStateException.class,
            () -> attempt.markSucceeded("AWB123456")
        );

        assertEquals("UNKNOWN", attempt.getAttemptStatus());
    }

    @Test
    void failedCannotBecomeSucceeded() {
        var attempt = processingAttempt();
        attempt.markFailed("Confirmed rejection");

        assertThrows(
            IllegalStateException.class,
            () -> attempt.markSucceeded("AWB123456")
        );

        assertEquals("FAILED", attempt.getAttemptStatus());
    }

    @Test
    void unknownCannotBecomeFailed() {
        var attempt = processingAttempt();
        attempt.markUnknown("Timeout");

        assertThrows(
            IllegalStateException.class,
            () -> attempt.markFailed("Late failure")
        );

        assertEquals("UNKNOWN", attempt.getAttemptStatus());
    }

    @Test
    void invalidWaybillRejected() {
        var attempt = processingAttempt();

        assertThrows(
            IllegalArgumentException.class,
            () -> attempt.markSucceeded("!")
        );

        assertEquals("PROCESSING", attempt.getAttemptStatus());
    }

    @Test
    void failureReasonTruncated() {
        var attempt = processingAttempt();

        attempt.markUnknown("X".repeat(600));

        assertEquals(500, attempt.getFailureReason().length());
    }
}