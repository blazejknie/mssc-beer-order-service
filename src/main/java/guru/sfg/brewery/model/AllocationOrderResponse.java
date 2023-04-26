package guru.sfg.brewery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationOrderResponse {
    private BeerOrderDto beerOrderDto;
    private Boolean isAllocationError = false;
    private Boolean isPendingInventory = false;
}
