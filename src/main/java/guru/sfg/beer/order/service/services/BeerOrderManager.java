package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;

public interface BeerOrderManager {
    String BEER_ORDER_HEADER = "BEER_ORDER_ID";

    BeerOrder newBeerOrder(BeerOrder beerOrder);
}
