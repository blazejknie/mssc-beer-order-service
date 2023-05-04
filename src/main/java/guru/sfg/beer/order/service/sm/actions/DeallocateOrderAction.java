package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.events.DeallocateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeallocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {
    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository repository;
    private final BeerOrderMapper mapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        String orderId = (String) context.getMessage().getHeaders().get(BeerOrderManager.BEER_ORDER_HEADER);
        BeerOrder beerOrder = getBeerOrder(orderId);

        DeallocateOrderRequest request = DeallocateOrderRequest.builder()
                                                             .beerOrderDto(mapper.beerOrderToDto(beerOrder))
                                                             .build();
        jmsTemplate.convertAndSend(JmsConfig.DEALLOCATE_ORDER_QUEUE_NAME, request);
        log.debug("Sent Deallocation Request for order id :" + orderId);
    }

    private BeerOrder getBeerOrder(String orderId) {
        return repository.findById(UUID.fromString(orderId))
                         .orElseThrow(() -> new RuntimeException("Order not found for validarion!!! Id: " + orderId));
    }
}
