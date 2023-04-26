package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.OrderStatusChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final OrderStatusChangeInterceptor orderStatusChangeInterceptor;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedOrder = beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedOrder;
    }

    @Override
    public void processValidationResponse(UUID orderId, Boolean isValid) {
        BeerOrder beerOrder = getBeerOrder(orderId);
        if (isValid) {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

            BeerOrder validatedOrder = getBeerOrder(orderId);

            sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);
        } else {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
        }
    }


    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = getBeerOrder(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = getBeerOrder(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
        updateAllocatedQty(beerOrderDto);

    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = getBeerOrder(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
        updateAllocatedQty(beerOrderDto);

    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = getBeerOrder(beerOrderDto.getId());
        List<BeerOrderLineDto> lineDtos = beerOrderDto.getBeerOrderLines();
        beerOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            lineDtos.forEach(dtoLine -> {
                if (dtoLine.getId().equals(beerOrderLine.getId())) {
                    beerOrderLine.setQuantityAllocated(dtoLine.getQuantityAllocated());
                }
            });
        });
        beerOrderRepository.saveAndFlush(beerOrder);
    }

    private BeerOrder getBeerOrder(UUID id) {
        return beerOrderRepository.findById(id)
                                  .orElseThrow(() -> new RuntimeException(
                                                         "Beer Order not exists. id: " + id));
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sendBeerOrderEvent(BeerOrder order, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(order);

        Message<BeerOrderEventEnum> msg = MessageBuilder.withPayload(eventEnum).setHeader(BeerOrderManager.BEER_ORDER_HEADER, order.getId().toString()).build();

        sm.sendEvent(Mono.just(msg)).subscribe();
        return sm;
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(
                beerOrder.getId());

        sm.stopReactively().subscribe();

        sm.getStateMachineAccessor()
          .doWithAllRegions(ama -> {
              ama.addStateMachineInterceptor(orderStatusChangeInterceptor);
              ama.resetStateMachineReactively(
                      new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null)).subscribe();

                  });

        sm.startReactively().subscribe();

        return sm;
    }
}
