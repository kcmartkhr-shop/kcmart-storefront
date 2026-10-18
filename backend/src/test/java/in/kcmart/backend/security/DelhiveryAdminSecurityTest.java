package in.kcmart.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryAdminSecurityTest {

    private static final String PATH =
        "/api/v1/admin/delhivery/track/TEST-AWB-123";

    @Test
    void delhiveryPathMatchesAdminSecurityPattern() {

        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", PATH);

        assertTrue(
            PathPatternRequestMatcher
                .withDefaults()
                .matcher("/api/v1/admin/**")
                .matches(request)
        );
    }

    @Test
    void adminAuthorityIsRecognized() {

        var authentication =
            new UsernamePasswordAuthenticationToken(
                "admin",
                "unused",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );

        assertTrue(
            authentication.getAuthorities().stream()
                .anyMatch(a ->
                    "ROLE_ADMIN".equals(a.getAuthority()))
        );
    }

    @Test
    void nonAdminDoesNotHaveAdminAuthority() {

        var authentication =
            new UsernamePasswordAuthenticationToken(
                "customer",
                "unused",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

        assertFalse(
            authentication.getAuthorities().stream()
                .anyMatch(a ->
                    "ROLE_ADMIN".equals(a.getAuthority()))
        );
    }
}