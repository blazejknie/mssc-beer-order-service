package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.OrderStatusChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
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
        BeerOrder savedOrder = beerOrderRepository.saveAndFlush(beerOrder);
        log.debug("saved new Beer Order: " + beerOrder);
        sendBeerOrderEvent(savedOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedOrder;
    }

    @Override
    @Transactional
    public void processValidationResponse(UUID orderId, Boolean isValid) {

        BeerOrder beerOrder = getBeerOrder(orderId);
        if (isValid) {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

            awaitForStatus(orderId, BeerOrderStatusEnum.VALIDATED);

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
        awaitForStatus(beerOrder.getId(), BeerOrderStatusEnum.ALLOCATED);
        updateAllocatedQty(beerOrderDto);

    }

    @Override
    public void cancelBeerOrder(UUID orderId) {
        BeerOrder beerOrder = getBeerOrder(orderId);
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER);
    }

    @Override
    public void pickupBeerOrder(UUID orderId) {
        Optional<BeerOrder> orderOptional = beerOrderRepository.findById(orderId);
        orderOptional.ifPresentOrElse(
                beerOrder -> sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.BEERORDER_PICKED_UP),
                () -> log.error("No Beer Order for id:" + orderId));
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
                .orElseThrow(() -> new RuntimeException("Beer Order not exists. id: " + id));
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sendBeerOrderEvent(BeerOrder order,
                                                                                     BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(order);

        Message<BeerOrderEventEnum> msg = MessageBuilder.withPayload(eventEnum)
                .setHeader(BeerOrderManager.BEER_ORDER_HEADER,
                        order.getId().toString())
                .build();

        sm.sendEvent(Mono.just(msg)).subscribe();
        return sm;
    }

    private void awaitForStatus(UUID beerOrderId, BeerOrderStatusEnum statusEnum) {
        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop Retries Exceeded");
            }

            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
                if (beerOrder.getOrderStatus().equals(statusEnum)) {
                    found.set(true);
                    log.debug("Order Found");
                } else {
                    log.debug(
                            "Order Status Not Equal. Expected: " + statusEnum + " Found: " + beerOrder.getOrderStatus()
                                    .name());
                }
            }, () -> log.debug("order Id not Found"));

            if (!found.get()) {
                try {
                    log.debug("Sleeping For Retry");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(
                beerOrder.getId());

        sm.stopReactively().subscribe();

        sm.getStateMachineAccessor().doWithAllRegions(ama -> {
            ama.addStateMachineInterceptor(orderStatusChangeInterceptor);
            ama.resetStateMachineReactively(
                    new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null)).subscribe();

        });

        sm.startReactively().subscribe();

        return sm;
    }
}
