package guru.sfg.beer.order.service.services.beer;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {
  Optional<BeerDto> fetchBeerByUpc(String upc);

  Optional<BeerDto> fetchBeerById(UUID id);
}
