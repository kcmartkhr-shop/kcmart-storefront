package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.service.DelhiveryPackageService;

import org.junit.jupiter.api.Test;
import in.kcmart.backend.security.SecurityConfig;
import in.kcmart.backend.security.InternalApiKeyFilter;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = AdminDelhiveryPackageController.class,
    properties = {
        "kcmart.admin.username=test-admin",
        "kcmart.admin.password=test-password"
    }
)
@EnableWebSecurity
@Import({
    SecurityConfig.class,
    InternalApiKeyFilter.class
})
class AdminDelhiveryPackageHttpSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DelhiveryPackageService service;

    private static final String URL =
        "/api/v1/admin/orders/5/delhivery-package";

    @Test
    void wrongAdminPasswordIsRejected() throws Exception {
        mvc.perform(get(URL)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .httpBasic("test-admin", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAdminCanReachPackageEndpoint() throws Exception {
        org.mockito.Mockito.when(service.getByOrderId(5L))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Package not found"));

        mvc.perform(get(URL)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .httpBasic("test-admin", "test-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    void authenticatedNonAdminIsForbidden() throws Exception {
        mvc.perform(get(URL)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("non-admin").roles("USER")))
                .andExpect(status().isForbidden());
    }
    @Test
    void unauthenticatedReadIsRejected() throws Exception {
        mvc.perform(get(URL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedCreateIsRejected() throws Exception {
        mvc.perform(post(URL)
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPackageRequestIsRejected() throws Exception {
        mvc.perform(post(URL)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("test-admin", "test-password"))
                .contentType("application/json")
                .content("""
                    {
                      "actualWeightGrams": 0,
                      "lengthCm": -1,
                      "widthCm": 20,
                      "heightCm": 15
                    }
                    """))
            .andExpect(status().isBadRequest());
    }
}