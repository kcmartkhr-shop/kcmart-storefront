ALTER TABLE razorpay_webhook_events
    DROP CONSTRAINT IF EXISTS chk_razorpay_webhook_processing_status;