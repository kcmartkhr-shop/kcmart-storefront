package in.kcmart.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryServiceabilityHttpSecurityTest {

    private static final String PATH =
        "/api/v1/admin/delhivery/serviceability/751001";

    private FilterChainProxy chain;

    @BeforeEach
    void setup() {
        var encoder = new BCryptPasswordEncoder();

        UserDetailsService users = new InMemoryUserDetailsManager(
            User.withUsername("test-admin")
                .password(encoder.encode("test-admin-password"))
                .roles("ADMIN")
                .build(),
            User.withUsername("test-user")
                .password(encoder.encode("test-user-password"))
                .roles("USER")
                .build()
        );

        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);

        AuthenticationManager manager =
            new ProviderManager(provider);

        var entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("KCMART Test");
        entryPoint.afterPropertiesSet();

        var basic = new BasicAuthenticationFilter(manager, entryPoint);

        var authorization = new AuthorizationFilter(
            AuthorityAuthorizationManager.hasRole("ADMIN")
        );

        SecurityFilterChain filterChain =
            new org.springframework.security.web.DefaultSecurityFilterChain(
                PathPatternRequestMatcher.withDefaults()
                    .matcher("/api/v1/admin/**"),
                new SecurityContextHolderFilter(
                    new org.springframework.security.web.context.HttpSessionSecurityContextRepository()
                ),
                new ExceptionTranslationFilter(entryPoint),
                basic,
                authorization
            );

        chain = new FilterChainProxy(filterChain);
    }

    private MockHttpServletResponse request(
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

        chain.doFilter(request, response, (req, res) -> {
            ((HttpServletResponse) res).setStatus(200);
        });

        return response;
    }

    @Test
    void anonymousRequestReturns401() throws Exception {
        assertEquals(401, request(null, null).getStatus());
    }

    @Test
    void nonAdminRequestReturns403() throws Exception {
        assertEquals(
            403,
            request("test-user", "test-user-password").getStatus()
        );
    }

    @Test
    void adminRequestPassesAuthorization() throws Exception {
        assertEquals(
            200,
            request("test-admin", "test-admin-password").getStatus()
        );
    }
}