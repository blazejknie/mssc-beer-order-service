package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.AllocationFailureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {
    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        String orderId = (String) context.getMessage().getHeaders().get(BeerOrderManager.BEER_ORDER_HEADER);

        AllocationFailureEvent failureEvent = AllocationFailureEvent.builder().orderId(UUID.fromString(
                Objects.requireNonNull(orderId))).build();

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_FAILURE_QUEUE, failureEvent);

        log.debug("Sent Allocation failure to queue for order id: " + orderId);
    }
}
