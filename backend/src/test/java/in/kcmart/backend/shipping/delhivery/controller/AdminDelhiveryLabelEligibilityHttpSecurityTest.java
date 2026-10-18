package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.security.SecurityConfig;
import in.kcmart.backend.shipping.delhivery.service.DelhiveryLabelEligibilityService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = AdminDelhiveryLabelEligibilityController.class,
    properties = {
        "kcmart.admin.username=test-admin",
        "kcmart.admin.password=test-only-password"
    }
)
@EnableWebSecurity
@Import(SecurityConfig.class)
class AdminDelhiveryLabelEligibilityHttpSecurityTest {

    private static final String URL =
        "/api/v1/admin/orders/10/delhivery-label-eligibility";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DelhiveryLabelEligibilityService service;

    @Test
    void anonymousRequestReturns401() throws Exception {
        mvc.perform(get(URL))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void nonAdminRequestReturns403() throws Exception {
        mvc.perform(get(URL)
                .with(user("ordinary-user").roles("USER")))
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminCanReadEligibleResult() throws Exception {
        when(service.check(10L)).thenReturn(
            new DelhiveryLabelEligibilityService.LabelEligibilityResult(
                true, null
            )
        );

        mvc.perform(get(URL)
                .with(user("test-admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(header().string(
                "Cache-Control",
                org.hamcrest.Matchers.containsString("no-store")
            ))
            .andExpect(jsonPath("$.eligible").value(true));

        verify(service).check(10L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void adminCanReadBlockedResult() throws Exception {
        when(service.check(10L)).thenReturn(
            new DelhiveryLabelEligibilityService.LabelEligibilityResult(
                false, "Delhivery booking is not confirmed."
            )
        );

        mvc.perform(get(URL)
                .with(user("test-admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eligible").value(false))
            .andExpect(jsonPath("$.blocker")
                .value("Delhivery booking is not confirmed."));

        verify(service).check(10L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void invalidOrderIdReturns400() throws Exception {
        mvc.perform(get(
                "/api/v1/admin/orders/0/delhivery-label-eligibility"
            ).with(user("test-admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void nonNumericOrderIdReturns400() throws Exception {
        mvc.perform(get(
                "/api/v1/admin/orders/invalid/delhivery-label-eligibility"
            ).with(user("test-admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}