package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateBeerOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationTestListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE_NAME)
    public void listen(@Payload ValidateBeerOrderRequest request) {
        String orderStatusCallbackUrl = request.getBeerOrderDto().getOrderStatusCallbackUrl();
        boolean isValid = orderStatusCallbackUrl == null || !orderStatusCallbackUrl.equals("failed-validation");

        log.debug("test validation request listener ran!");

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESULT_QUEUE_NAME,
                ValidateBeerOrderResponse.builder().isValid(isValid).orderId(request.getBeerOrderDto().getId()).build());
    }
}
