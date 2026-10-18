package in.kcmart.backend.customer.repository;

import in.kcmart.backend.customer.entity.Customer;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository
    extends JpaRepository<Customer, Long> {

    Optional<Customer> findByProviderAndProviderSubject(
        String provider,
        String providerSubject
    );

    Optional<Customer> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select customer
        from Customer customer
        where lower(customer.email) = lower(:email)
        """)
    Optional<Customer> findByEmailIgnoreCaseForUpdate(
        @Param("email") String email
    );
}