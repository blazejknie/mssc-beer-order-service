package guru.sfg.beer.order.service.services.beer;

import guru.sfg.brewery.model.BeerDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled
@SpringBootTest
class BeerServiceRestTemplateImplTest {

    @Autowired
    BeerService beerService;

    @Test
    void fetchBeerByUpc() {
        BeerDto beerDto = beerService.fetchBeerByUpc("0631234200036").get();

        System.out.println(beerDto);
    }
}