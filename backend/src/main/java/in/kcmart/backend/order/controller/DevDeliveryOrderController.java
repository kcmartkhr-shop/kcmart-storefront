package in.kcmart.backend.order.controller;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.service.DevDeliveryOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dev/delivery-orders")
public class DevDeliveryOrderController {

    private final DevDeliveryOrderService service;

    public DevDeliveryOrderController(
            DevDeliveryOrderService service) {

        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create() {
        return service.createTestDeliveryOrder();
    }
}