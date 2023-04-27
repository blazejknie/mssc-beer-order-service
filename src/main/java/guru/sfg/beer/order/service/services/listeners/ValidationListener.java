package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateBeerOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationListener {
    private final BeerOrderManager manager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESULT_QUEUE_NAME)
    public void handleValidationResponse(ValidateBeerOrderResponse response) {
        UUID orderId = response.getOrderId();
        log.debug("Validation Result for Order Id: " + orderId);

        manager.processValidationResponse(orderId, response.getIsValid());
    }
}
