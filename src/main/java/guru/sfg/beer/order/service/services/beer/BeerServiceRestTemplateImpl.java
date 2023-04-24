package guru.sfg.beer.order.service.services.beer;

import guru.sfg.brewery.model.BeerDto;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Component
@ConfigurationProperties(prefix = "blazej.brewery", ignoreUnknownFields = false)
public class BeerServiceRestTemplateImpl implements BeerService {

    private static final String BEER_PATH_UPC = "/api/v1/beerUpc/";
    private static final String BEER_PATH_ID = "/api/v1/beer/";
    private final RestTemplate restTemplate;

    private String beerServiceHost;

    public BeerServiceRestTemplateImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setBeerServiceHost(String beerServiceHost) {
        this.beerServiceHost = beerServiceHost;
    }

    @Override
    public Optional<BeerDto> fetchBeerByUpc(String upc) {
    return Optional.ofNullable(restTemplate.getForObject(beerServiceHost + BEER_PATH_UPC + upc, BeerDto.class));

    }

    @Override
    public Optional<BeerDto> fetchBeerById(UUID id) {
       return Optional.ofNullable(restTemplate.getForObject(beerServiceHost + BEER_PATH_UPC + id, BeerDto.class));
    }
}
