package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class DelhiveryFailClosedTransportTest {

    @Test
    void pdfDownloaderDefaultTransportFailsClosed() {
        var downloader = new DelhiveryLabelHttpPdfDownloader();

        IOException error = assertThrows(
            IOException.class,
            () -> downloader.download(
                "https://s3.amazonaws.com/example/label.pdf"
            )
        );

        assertTrue(
            error.getMessage().contains(
                "DNS-pinned HTTPS transport"
            )
        );
    }

    @Test
    void packingSlipDefaultTransportFailsClosed() {
        var client = new DelhiveryPackingSlipHttpClient();

        IOException error = assertThrows(
            IOException.class,
            () -> client.fetchJson(
                "https://staging-express.delhivery.com",
                "dummy-test-token",
                "123456789012"
            )
        );

        assertTrue(
            error.getMessage().contains(
                "DNS-pinned HTTPS transport"
            )
        );
    }
}