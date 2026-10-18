package in.kcmart.backend.security;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ActualSecurityConfigContextTest {

    private static final String PATH =
        "/api/v1/admin/delhivery/track/TEST-AWB-123";

    @Configuration
    @EnableWebSecurity
    static class TestSecurityInfrastructure { }

    private final WebApplicationContextRunner runner =
        new WebApplicationContextRunner()
            .withUserConfiguration(
                SecurityConfig.class,
                TestSecurityInfrastructure.class
            )
            .withBean(
                InternalApiKeyFilter.class,
                () -> new InternalApiKeyFilter("test-only-internal-key")
            )
            .withPropertyValues(
                "kcmart.admin.username=test-admin",
                "kcmart.admin.password=test-admin-password"
            );

    private int request(
            Filter filter,
            String username,
            String password) throws Exception {

        var request = new MockHttpServletRequest("GET", PATH);
        var response = new MockHttpServletResponse();

        if (username != null) {
            String credentials = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
            );
            request.addHeader("Authorization", "Basic " + encoded);
        }

        filter.doFilter(request, response, (req, res) -> {
            ((jakarta.servlet.http.HttpServletResponse) res)
                .setStatus(200);
        });

        return response.getStatus();
    }

    @Test
    void actualSecurityFilterChainProtectsAdminEndpoint() {

        runner.run(context -> {

            assertNull(context.getStartupFailure());

            assertFalse(context.containsBean("dataSource"));

            assertNotNull(
                context.getBean(SecurityFilterChain.class)
            );

            Filter filter = context.getBean(
                "springSecurityFilterChain",
                Filter.class
            );

            assertEquals(
                401,
                request(filter, null, null),
                "Anonymous access must be rejected"
            );

            assertEquals(
                200,
                request(
                    filter,
                    "test-admin",
                    "test-admin-password"
                ),
                "Admin must pass authorization"
            );

            UserDetailsService users =
                context.getBean(UserDetailsService.class);

            assertTrue(users instanceof InMemoryUserDetailsManager);

            PasswordEncoder encoder =
                context.getBean(PasswordEncoder.class);

            ((InMemoryUserDetailsManager) users).createUser(
                User.withUsername("test-customer")
                    .password(encoder.encode("test-customer-password"))
                    .roles("USER")
                    .build()
            );

            assertEquals(
                403,
                request(
                    filter,
                    "test-customer",
                    "test-customer-password"
                ),
                "Non-admin access must be rejected"
            );
        });
    }
}