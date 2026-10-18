package in.kcmart.backend.shipping.delhivery.client;

import org.springframework.stereotype.Component;

@Component

public class DisabledDelhiveryLabelClient implements DelhiveryLabelClient {

    @Override
    public byte[] fetchLabelPdf(String waybill) {
        throw new IllegalStateException(
            "Delhivery label retrieval is disabled."
        );
    }
}