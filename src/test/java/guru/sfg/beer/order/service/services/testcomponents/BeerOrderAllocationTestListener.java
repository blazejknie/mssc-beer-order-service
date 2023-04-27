package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.AllocationOrderResponse;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BeerOrderAllocationTestListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE_NAME)
    public void listen(AllocateOrderRequest request) {
        BeerOrderDto beerOrderDto = request.getBeerOrderDto();
        beerOrderDto.getBeerOrderLines().forEach(line -> line.setQuantityAllocated(line.getOrderQuantity()));
        AllocationOrderResponse response = AllocationOrderResponse.builder()
                                                               .beerOrderDto(beerOrderDto)
                                                               .isAllocationError(false)
                                                               .isPendingInventory(false)
                                                               .build();

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE_NAME, response);
    }
}
