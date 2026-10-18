package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryLabelClientBeanSafetyTest {

    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner()
            .withUserConfiguration(
                DisabledDelhiveryLabelClient.class
            );

    @Test
    void disabledByDefaultRegistersSafeClient() {
        contextRunner.run(context -> {
            assertFalse(context.getStartupFailure() != null);
            assertEquals(
                1,
                context.getBeansOfType(
                    DelhiveryLabelClient.class
                ).size()
            );
            assertInstanceOf(
                DisabledDelhiveryLabelClient.class,
                context.getBean(DelhiveryLabelClient.class)
            );
        });
    }

    @Test
    void enabledFlagStillRegistersFailClosedClient() {
        contextRunner
            .withPropertyValues(
                "delhivery.label-retrieval-enabled=true"
            )
            .run(context -> {
                assertNull(context.getStartupFailure());
                DelhiveryLabelClient client =
                    context.getBean(DelhiveryLabelClient.class);

                assertInstanceOf(
                    DisabledDelhiveryLabelClient.class,
                    client
                );

                assertThrows(
                    IllegalStateException.class,
                    () -> client.fetchLabelPdf("1234567890123")
                );
            });
    }

    @Test
    void disabledFlagRejectsRetrieval() {
        contextRunner
            .withPropertyValues(
                "delhivery.label-retrieval-enabled=false"
            )
            .run(context -> {
                DelhiveryLabelClient client =
                    context.getBean(DelhiveryLabelClient.class);

                assertThrows(
                    IllegalStateException.class,
                    () -> client.fetchLabelPdf("1234567890123")
                );
            });
    }
}