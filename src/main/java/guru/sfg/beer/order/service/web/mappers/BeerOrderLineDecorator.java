package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.services.beer.BeerDto;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.beer.order.service.web.model.BeerOrderLineDto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public abstract class BeerOrderLineDecorator implements BeerOrderLineMapper {

  BeerOrderLineMapper mapper;

  BeerService beerService;

  @Autowired
  public void setMapper(BeerOrderLineMapper mapper) {
    this.mapper = mapper;
  }

  @Autowired
  public void setBeerService(BeerService beerService) {
    this.beerService = beerService;
  }

  @Override
  public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
      Optional<BeerDto> beerDtoOptional = beerService.fetchBeerByUpc(line.getUpc());

      BeerOrderLineDto beerOrderLineDto = mapper.beerOrderLineToDto(line);

      beerDtoOptional.ifPresent(beerDto -> {
        beerOrderLineDto.setBeerName(beerDto.getBeerName());
        beerOrderLineDto.setBeerStyle(beerDto.getBeerStyle().name());
        beerOrderLineDto.setPrice(beerDto.getPrice());
        beerOrderLineDto.setBeerId(beerDto.getId());
      });

      return beerOrderLineDto;
  }

  @Override
  public BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto) {
    return mapper.dtoToBeerOrderLine(dto);
  }
}
