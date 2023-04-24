package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.OrderStatusChangeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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

    private void sendBeerOrderEvent(BeerOrder order, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(order);

        Message<BeerOrderEventEnum> msg = MessageBuilder.withPayload(eventEnum).setHeader(BeerOrderManager.BEER_ORDER_HEADER, order.getId().toString()).build();

        sm.sendEvent(Mono.just(msg)).subscribe();
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
