package in.kcmart.backend.customer.service;

import in.kcmart.backend.customer.dto.CustomerResponse;
import in.kcmart.backend.customer.entity.Customer;
import in.kcmart.backend.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(
        CustomerRepository customerRepository
    ) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> findById(Long id) {
        return customerRepository
            .findById(id)
            .filter(Customer::isActive)
            .map(CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> findByProviderIdentity(
        String provider,
        String providerSubject
    ) {
        return customerRepository
            .findByProviderAndProviderSubject(
                normalizeProvider(provider),
                requireText(
                    providerSubject,
                    "providerSubject"
                )
            )
            .filter(Customer::isActive)
            .map(CustomerResponse::from);
    }

    /*
     * IMPORTANT:
     *
     * This method must only be called after the external
     * identity has been verified by a trusted server-side
     * authentication bridge.
     *
     * It is deliberately NOT exposed directly to the browser.
     */
    @Transactional
    public CustomerResponse synchronizeTrustedIdentity(
        String provider,
        String providerSubject,
        String email,
        String name,
        String imageUrl
    ) {
        String normalizedProvider =
            normalizeProvider(provider);

        String normalizedSubject =
            requireText(
                providerSubject,
                "providerSubject"
            );

        String normalizedEmail =
            normalizeEmail(email);

        Optional<Customer> existingIdentity =
            customerRepository
                .findByProviderAndProviderSubject(
                    normalizedProvider,
                    normalizedSubject
                );

        if (existingIdentity.isPresent()) {
            Customer customer = existingIdentity.get();

            Optional<Customer> emailOwner =
                customerRepository
                    .findByEmailIgnoreCase(
                        normalizedEmail
                    );

            if (
                emailOwner.isPresent()
                && !emailOwner
                    .get()
                    .getId()
                    .equals(customer.getId())
            ) {
                throw new IllegalStateException(
                    "Email is already linked to another customer."
                );
            }

            customer.updateProfile(
                normalizedEmail,
                normalizeNullable(name),
                normalizeNullable(imageUrl)
            );

            customer.activate();

            return CustomerResponse.from(
                customerRepository.save(customer)
            );
        }

        Optional<Customer> existingEmail =
            customerRepository
                .findByEmailIgnoreCase(
                    normalizedEmail
                );

        if (existingEmail.isPresent()) {
            throw new IllegalStateException(
                "Email already exists with a different provider identity."
            );
        }

        Customer customer = new Customer(
            normalizedProvider,
            normalizedSubject,
            normalizedEmail,
            normalizeNullable(name),
            normalizeNullable(imageUrl)
        );

        return CustomerResponse.from(
            customerRepository.save(customer)
        );
    }

    private String normalizeProvider(
        String provider
    ) {
        return requireText(
            provider,
            "provider"
        ).toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(
        String email
    ) {
        String value = requireText(
            email,
            "email"
        ).toLowerCase(Locale.ROOT);

        if (
            !value.contains("@")
            || value.startsWith("@")
            || value.endsWith("@")
        ) {
            throw new IllegalArgumentException(
                "Invalid email address."
            );
        }

        return value;
    }

    private String requireText(
        String value,
        String fieldName
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                fieldName + " is required."
            );
        }

        return value.trim();
    }

    private String normalizeNullable(
        String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }
}