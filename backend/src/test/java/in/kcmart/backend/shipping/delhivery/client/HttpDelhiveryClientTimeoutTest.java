package in.kcmart.backend.shipping.delhivery.client;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class HttpDelhiveryClientTimeoutTest {

    @Test
    void clientUsesConfiguredRequestFactory() throws Exception {

        DelhiveryProperties properties = new DelhiveryProperties(
            true,
            "http://127.0.0.1:1",
            "mock-test-token",
            "MOCK-PICKUP"
        );

        HttpDelhiveryClient client = new HttpDelhiveryClient(
            RestClient.builder(),
            properties
        );

        Field clientField = HttpDelhiveryClient.class
            .getDeclaredField("restClient");
        clientField.setAccessible(true);

        RestClient restClient = (RestClient) clientField.get(client);

        Field factoryField = restClient.getClass()
            .getDeclaredField("clientRequestFactory");
        factoryField.setAccessible(true);

        ClientHttpRequestFactory factory =
            (ClientHttpRequestFactory) factoryField.get(restClient);

        assertInstanceOf(
            SimpleClientHttpRequestFactory.class,
            factory
        );
    }
}