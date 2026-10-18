package in.kcmart.backend.customer.dto;

import in.kcmart.backend.customer.entity.Customer;

import java.time.OffsetDateTime;

public record CustomerResponse(
    Long id,
    String email,
    String name,
    String imageUrl,
    boolean active,
    OffsetDateTime createdAt
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getEmail(),
            customer.getName(),
            customer.getImageUrl(),
            customer.isActive(),
            customer.getCreatedAt()
        );
    }
}