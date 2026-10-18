package in.kcmart.backend.shipping.delhivery.client;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryClientActivationTest {

    private final ApplicationContextRunner runner =
        new ApplicationContextRunner()
            .withUserConfiguration(ClientTestConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @Import({
        DelhiveryProperties.class,
        DisabledDelhiveryClient.class,
        HttpDelhiveryClient.class
    })
    static class ClientTestConfiguration {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }

    @Test
    void disabledConfigurationSelectsDisabledClient() {
        runner.withPropertyValues("delhivery.enabled=false")
            .run(context -> {
                assertNull(context.getStartupFailure());
                assertEquals(1, context.getBeansOfType(DelhiveryClient.class).size());
                assertInstanceOf(
                    DisabledDelhiveryClient.class,
                    context.getBean(DelhiveryClient.class)
                );
            });
    }

    @Test
    void missingConfigurationDefaultsToDisabledClient() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertInstanceOf(
                DisabledDelhiveryClient.class,
                context.getBean(DelhiveryClient.class)
            );
        });
    }

    @Test
    void enabledConfigurationSelectsHttpClient() {
        runner.withPropertyValues(
                "delhivery.enabled=true",
                "delhivery.base-url=https://example.invalid"
            )
            .run(context -> {
                assertNull(context.getStartupFailure());
                assertEquals(1, context.getBeansOfType(DelhiveryClient.class).size());
                assertInstanceOf(
                    HttpDelhiveryClient.class,
                    context.getBean(DelhiveryClient.class)
                );
            });
    }
}