package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.events.AllocationFailureEvent;
import guru.sfg.brewery.model.events.DeallocateOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class BeerOrderManagerImplIT {

    @Autowired
    BeerService beerService;

    @Autowired
    Environment environment;

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JmsTemplate jmsTemplate;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        beerService.setBeerServiceHost("http://localhost:" + this.environment.getProperty("wiremock.server.port"));
        testCustomer = customerRepository.save(Customer.builder().customerName("Test Customer").build());
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException {
        BeerDto test_beer = BeerDto.builder().id(beerId).upc("12345").beerName("test beer").build();
        String testBeerAsText = objectMapper.writeValueAsString(test_beer);

        stubFor(get(urlEqualTo("/api/v1/beerUpc/" + "12345")).willReturn(okJson(testBeerAsText)));
        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder checkOrder = beerOrderRepository.findById(beerOrder.getId())
                                                      .orElseThrow(() -> new RuntimeException(""));
            BeerOrderLine line = checkOrder.getBeerOrderLines().iterator().next();
            assertNotNull(checkOrder);
            assertEquals(BeerOrderStatusEnum.ALLOCATED, checkOrder.getOrderStatus());
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        savedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId())
                                            .orElseThrow(() -> new RuntimeException(""));

        assertNotNull(savedBeerOrder);

        assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder.getOrderStatus());
    }

    @Test
    void test_fail_validation() throws JsonProcessingException {
        BeerDto test_beer = BeerDto.builder().id(beerId).upc("12345").beerName("test beer").build();
        String testBeerAsText = objectMapper.writeValueAsString(test_beer);

        stubFor(get(urlEqualTo("/api/v1/beerUpc/" + "12345")).willReturn(okJson(testBeerAsText)));
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setOrderStatusCallbackUrl("failed-validation");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder checkOrder = beerOrderRepository.findById(beerOrder.getId())
                                                      .orElseThrow(() -> new RuntimeException(""));
            BeerOrderLine line = checkOrder.getBeerOrderLines().iterator().next();
            assertNotNull(checkOrder);
            assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, checkOrder.getOrderStatus());
        });
    }

    @Test
    void test_fail_allocation() throws JsonProcessingException {
        BeerDto test_beer = BeerDto.builder().id(beerId).upc("12345").beerName("test beer").build();
        String testBeerAsText = objectMapper.writeValueAsString(test_beer);

        stubFor(get(urlEqualTo("/api/v1/beerUpc/" + "12345")).willReturn(okJson(testBeerAsText)));
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setOrderStatusCallbackUrl("failed-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder checkOrder = beerOrderRepository.findById(beerOrder.getId())
                                                      .orElseThrow(() -> new RuntimeException(""));
            BeerOrderLine line = checkOrder.getBeerOrderLines().iterator().next();
            assertNotNull(checkOrder);
            assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, checkOrder.getOrderStatus());

        });

        AllocationFailureEvent event = (AllocationFailureEvent) jmsTemplate.receiveAndConvert(
                JmsConfig.ALLOCATE_ORDER_FAILURE_QUEUE);
        assertNotNull(event);
        assertThat(event.getOrderId()).isEqualTo(savedBeerOrder.getId());
    }

    @Test
    void test_partial_allocation() throws JsonProcessingException {
        BeerDto test_beer = BeerDto.builder().id(beerId).upc("12345").beerName("test beer").build();
        String testBeerAsText = objectMapper.writeValueAsString(test_beer);

        stubFor(get(urlEqualTo("/api/v1/beerUpc/" + "12345")).willReturn(okJson(testBeerAsText)));
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setOrderStatusCallbackUrl("partial-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder checkOrder = beerOrderRepository.findById(beerOrder.getId())
                                                      .orElseThrow(() -> new RuntimeException(""));
            BeerOrderLine line = checkOrder.getBeerOrderLines().iterator().next();
            assertNotNull(checkOrder);
            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY, checkOrder.getOrderStatus());
        });
    }

    @Test
    void test_allocated_to_pickedUp_when_valid_order_should_pass() {
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setOrderStatus(BeerOrderStatusEnum.ALLOCATED);
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);

        beerOrderManager.pickupBeerOrder(savedBeerOrder.getId());
        await().untilAsserted(() -> {
            BeerOrder order = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PICKED_UP, order.getOrderStatus());
        });

        BeerOrder result = beerOrderRepository.findById(savedBeerOrder.getId())
                                              .orElseThrow(() -> new RuntimeException(""));

        assertEquals(BeerOrderStatusEnum.PICKED_UP, result.getOrderStatus());
    }

    @Test
    void test_validation_pending_to_cancelled() throws JsonProcessingException {
        BeerDto test_beer = BeerDto.builder().id(beerId).upc("12345").beerName("test beer").build();
        String testBeerAsText = objectMapper.writeValueAsString(test_beer);

        stubFor(get(urlEqualTo("/api/v1/beerUpc/" + "12345")).willReturn(okJson(testBeerAsText)));
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setOrderStatusCallbackUrl("dont-validate");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder order = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.VALIDATION_PENDING, order.getOrderStatus());
        });

        beerOrderManager.cancelBeerOrder(savedBeerOrder.getId());

        await().untilAsserted(() -> {
                    BeerOrder order = beerOrderRepository.findById(savedBeerOrder.getId()).get();
                    assertEquals(BeerOrderStatusEnum.CANCELLED, order.getOrderStatus());
                });
    }

    @Test
    void test_allocation_pending_to_cancelled() throws JsonProcessingException {
        BeerDto test_beer = BeerDto.builder().id(beerId).upc("12345").beerName("test beer").build();
        String testBeerAsText = objectMapper.writeValueAsString(test_beer);

        stubFor(get(urlEqualTo("/api/v1/beerUpc/" + "12345")).willReturn(okJson(testBeerAsText)));
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setOrderStatusCallbackUrl("allocation-cancelled");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder order = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATION_PENDING, order.getOrderStatus());
        });

        beerOrderManager.cancelBeerOrder(savedBeerOrder.getId());

        await().untilAsserted(() -> {
                    BeerOrder order = beerOrderRepository.findById(savedBeerOrder.getId()).get();
                    assertEquals(BeerOrderStatusEnum.CANCELLED, order.getOrderStatus());
                });
    }

    @Test
    void test_allocated_to_cancelled() throws JsonProcessingException {
        BeerDto test_beer = BeerDto.builder().id(beerId).upc("12345").beerName("test beer").build();
        String testBeerAsText = objectMapper.writeValueAsString(test_beer);

        stubFor(get(urlEqualTo("/api/v1/beerUpc/" + "12345")).willReturn(okJson(testBeerAsText)));
        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder checkOrder = beerOrderRepository.findById(beerOrder.getId())
                                                      .orElseThrow(() -> new RuntimeException(""));
            BeerOrderLine line = checkOrder.getBeerOrderLines().iterator().next();
            assertNotNull(checkOrder);
            assertEquals(BeerOrderStatusEnum.ALLOCATED, checkOrder.getOrderStatus());
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });


        beerOrderManager.cancelBeerOrder(savedBeerOrder.getId());

        await().untilAsserted(() -> {
                    BeerOrder order = beerOrderRepository.findById(savedBeerOrder.getId()).get();
                    assertEquals(BeerOrderStatusEnum.CANCELLED, order.getOrderStatus());
                });

        DeallocateOrderRequest request = (DeallocateOrderRequest) jmsTemplate.receiveAndConvert(
                JmsConfig.DEALLOCATE_ORDER_QUEUE_NAME);

        assertNotNull(request);
        assertThat(request.getBeerOrderDto().getId()).isEqualTo(savedBeerOrder.getId());
    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder().customer(testCustomer).build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder().beerId(beerId).upc("12345").orderQuantity(1).beerOrder(beerOrder).build());

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }


}
