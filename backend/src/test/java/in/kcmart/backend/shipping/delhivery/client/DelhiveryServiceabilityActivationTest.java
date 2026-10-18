package in.kcmart.backend.shipping.delhivery.client;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryServiceabilityActivationTest {

    private final ApplicationContextRunner runner =
        new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @Import({
        DelhiveryProperties.class,
        DelhiveryServiceabilityClient.class
    })
    static class TestConfiguration {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }

    @Test
    void defaultConfigurationBlocksServiceabilityRequest() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());

            DelhiveryServiceabilityClient client =
                context.getBean(DelhiveryServiceabilityClient.class);

            assertThrows(
                IllegalStateException.class,
                () -> client.fetchPincode("194103")
            );
        });
    }

    @Test
    void explicitDisabledConfigurationBlocksRequest() {
        runner.withPropertyValues("delhivery.enabled=false")
            .run(context -> {
                assertNull(context.getStartupFailure());

                DelhiveryServiceabilityClient client =
                    context.getBean(DelhiveryServiceabilityClient.class);

                assertThrows(
                    IllegalStateException.class,
                    () -> client.fetchPincode("194103")
                );
            });
    }

    @Test
    void enabledConfigurationRejectsMissingToken() {
        runner.withPropertyValues(
                "delhivery.enabled=true",
                "delhivery.api-token=",
                "delhivery.base-url=http://127.0.0.1:1"
            )
            .run(context -> {
                assertNull(context.getStartupFailure());

                DelhiveryServiceabilityClient client =
                    context.getBean(DelhiveryServiceabilityClient.class);

                assertThrows(
                    IllegalStateException.class,
                    () -> client.fetchPincode("194103")
                );
            });
    }
}