package in.kcmart.backend.shipping.delhivery.client;

import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryApacheDirectRouteTest {

    @Test
    void httpsRouteDoesNotUseProxy() throws Exception {
        var planner = new DefaultRoutePlanner(null);

        var target = new HttpHost(
            "https", "s3.amazonaws.com", 443
        );

        var route = planner.determineRoute(
            target, new BasicHttpContext()
        );

        assertNotNull(route);
        assertFalse(route.getProxyHost() != null);
        assertEquals("s3.amazonaws.com",
            route.getTargetHost().getHostName());
        assertTrue(route.isSecure());
    }

    @Test
    void httpRouteDoesNotUseProxy() throws Exception {
        var planner = new DefaultRoutePlanner(null);

        var target = new HttpHost(
            "http", "example.com", 80
        );

        var route = planner.determineRoute(
            target, new BasicHttpContext()
        );

        assertNull(route.getProxyHost());
        assertFalse(route.isSecure());
    }

    @Test
    void directPlannerIgnoresProxySystemProperties()
            throws Exception {

        String oldHost = System.getProperty("https.proxyHost");
        String oldPort = System.getProperty("https.proxyPort");

        try {
            System.setProperty(
                "https.proxyHost", "127.0.0.1"
            );
            System.setProperty(
                "https.proxyPort", "9999"
            );

            var planner = new DefaultRoutePlanner(null);

            var route = planner.determineRoute(
                new HttpHost(
                    "https", "s3.amazonaws.com", 443
                ),
                new BasicHttpContext()
            );

            assertNull(
                route.getProxyHost(),
                "Direct route planner must ignore system proxy"
            );
        }
        finally {
            if (oldHost == null) {
                System.clearProperty("https.proxyHost");
            }
            else {
                System.setProperty("https.proxyHost", oldHost);
            }

            if (oldPort == null) {
                System.clearProperty("https.proxyPort");
            }
            else {
                System.setProperty("https.proxyPort", oldPort);
            }
        }
    }
}