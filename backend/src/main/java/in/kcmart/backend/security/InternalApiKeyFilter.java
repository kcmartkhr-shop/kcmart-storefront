package in.kcmart.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME =
        "X-KCMART-Internal-Key";

    private static final String INTERNAL_PREFIX =
        "/api/v1/internal/";

    private final byte[] expectedKey;

    public InternalApiKeyFilter(
        @Value("${kcmart.internal.api-key}")
        String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "KCMART internal API key is not configured."
            );
        }

        this.expectedKey =
            apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(
        HttpServletRequest request
    ) {
        return !request
            .getRequestURI()
            .startsWith(INTERNAL_PREFIX);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String suppliedKey =
            request.getHeader(HEADER_NAME);

        if (
            suppliedKey == null
            || !MessageDigest.isEqual(
                expectedKey,
                suppliedKey.getBytes(StandardCharsets.UTF_8)
            )
        ) {
            response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED
            );

            return;
        }

        filterChain.doFilter(
            request,
            response
        );
    }
}