package in.kcmart.backend.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${kcmart.admin.username}") String username,
            @Value("${kcmart.admin.password}") String password,
            PasswordEncoder encoder) {

        var admin = User.withUsername(username)
                .password(encoder.encode(password))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalApiKeyFilter internalApiKeyFilter) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/categories",
                    "/api/v1/products",
                    "/api/v1/products/**", "/api/v1/product-images/**"
                ).permitAll()
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/customers/status"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/payments/razorpay/webhook"
                ).permitAll()
                .requestMatchers(
                    "/api/v1/admin/**",
                    "/api/v1/dev/**"
                ).hasRole("ADMIN")
                .requestMatchers(
                    "/api/v1/internal/**"
                ).permitAll()
                .anyRequest().denyAll()
            )
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(
                internalApiKeyFilter,
                BasicAuthenticationFilter.class
            );

        return http.build();
    }
}