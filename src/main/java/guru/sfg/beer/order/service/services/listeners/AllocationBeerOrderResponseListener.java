package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.AllocationOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationBeerOrderResponseListener {
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE_NAME)
    public void handleAllocationResponse(AllocationOrderResponse response) {
        log.debug("Allocation Result: " + response);

        if (!response.getIsAllocationError() && !response.getIsPendingInventory()) {
            beerOrderManager.beerOrderAllocationPassed(response.getBeerOrderDto());
        } else if (!response.getIsAllocationError() && response.getIsPendingInventory()) {
            beerOrderManager.beerOrderAllocationPendingInventory(response.getBeerOrderDto());
        } else if (response.getIsAllocationError()){
            beerOrderManager.beerOrderAllocationFailed(response.getBeerOrderDto());
        }
    }

}
