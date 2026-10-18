package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryLabelHttpPdfDownloaderTest {

    private static final String URL =
        "https://s3.amazonaws.com/test-bucket/label.pdf";

    private byte[] pdf() {
        return "%PDF-1.4\n1 0 obj\nendobj\n%%EOF\n"
            .getBytes(StandardCharsets.US_ASCII);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<InputStream> response(
            int status, byte[] body, long length) {

        HttpResponse<InputStream> response =
            mock(HttpResponse.class);

        HttpHeaders headers = HttpHeaders.of(
            Map.of("Content-Length", List.of(
                String.valueOf(length)
            )),
            (a, b) -> true
        );

        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(headers);
        when(response.body()).thenReturn(
            new ByteArrayInputStream(body)
        );

        return response;
    }

    @Test
    void downloadsValidPdf() throws Exception {
        var downloader = new DelhiveryLabelHttpPdfDownloader(
            request -> {
                assertEquals("GET", request.method());
                assertTrue(request.headers()
                    .firstValue("Authorization").isEmpty());

                return response(200, pdf(), pdf().length);
            }
        );

        assertArrayEquals(pdf(), downloader.download(URL));
    }

    @Test
    void rejectsRedirect() {
        var downloader = new DelhiveryLabelHttpPdfDownloader(
            request -> response(302, new byte[0], 0)
        );

        assertThrows(
            IOException.class,
            () -> downloader.download(URL)
        );
    }

    @Test
    void rejectsServerError() {
        var downloader = new DelhiveryLabelHttpPdfDownloader(
            request -> response(500, new byte[0], 0)
        );

        assertThrows(
            IOException.class,
            () -> downloader.download(URL)
        );
    }

    @Test
    void rejectsOversizedContentLength() {
        var downloader = new DelhiveryLabelHttpPdfDownloader(
            request -> response(
                200, pdf(), 5L * 1024 * 1024 + 1
            )
        );

        assertThrows(
            IOException.class,
            () -> downloader.download(URL)
        );
    }

    @Test
    void rejectsUnsafeHostBeforeTransport() {
        var transport =
            mock(DelhiveryLabelHttpPdfDownloader.Transport.class);

        var downloader =
            new DelhiveryLabelHttpPdfDownloader(transport);

        assertThrows(
            IllegalArgumentException.class,
            () -> downloader.download(
                "http://localhost/internal"
            )
        );

        verifyNoInteractions(transport);
    }

    @Test
    void rejectsInvalidPdfBody() {
        var downloader = new DelhiveryLabelHttpPdfDownloader(
            request -> response(
                200,
                "not a pdf".getBytes(StandardCharsets.US_ASCII),
                9
            )
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> downloader.download(URL)
        );
    }
}