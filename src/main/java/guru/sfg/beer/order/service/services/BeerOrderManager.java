package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.BeerOrderDto;

import java.util.UUID;

public interface BeerOrderManager {
    String BEER_ORDER_HEADER = "BEER_ORDER_ID";

    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void processValidationResponse(UUID orderId, Boolean isValid);

    void beerOrderAllocationFailed(BeerOrderDto beerOrderDto);

    void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto);

    void beerOrderAllocationPassed(BeerOrderDto beerOrderDto);
}
