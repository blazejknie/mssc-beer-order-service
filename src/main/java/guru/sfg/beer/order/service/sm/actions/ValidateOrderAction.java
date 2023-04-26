package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {
    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        Message<BeerOrderEventEnum> message = context.getMessage();
        String orderId = (String) message.getHeaders().get(BeerOrderManager.BEER_ORDER_HEADER);
        log.debug("Order Validation Process for Order with id: " + orderId);
        sendOrderValidationRequest(orderId);
    }


    private void sendOrderValidationRequest(String orderId) {
        BeerOrder beerOrder = getBeerOrder(orderId);

        BeerOrderDto beerOrderDto = beerOrderMapper.beerOrderToDto(beerOrder);

        Message<ValidateBeerOrderRequest> msg = MessageBuilder.withPayload(new ValidateBeerOrderRequest(beerOrderDto))
                                                              .build();

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_QUEUE_NAME, msg);
    }

    private BeerOrder getBeerOrder(String orderId) {
        return beerOrderRepository.findById(UUID.fromString(orderId))
                                  .orElseThrow(() -> new RuntimeException(
                                          "Order not found for validarion!!! Id: " + orderId));
    }
}
