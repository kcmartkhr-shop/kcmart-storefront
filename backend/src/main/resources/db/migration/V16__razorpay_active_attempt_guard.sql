CREATE UNIQUE INDEX uk_razorpay_one_created_attempt_per_order
    ON razorpay_payment_attempts(order_id)
    WHERE status = 'CREATED';