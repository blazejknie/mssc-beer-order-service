package guru.sfg.beer.order.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
@AllArgsConstructor
public class JmsConfig {
    private final ObjectMapper objectMapper;

    public static final String VALIDATE_ORDER_QUEUE_NAME = "validate-order";
    public static final String VALIDATE_ORDER_RESULT_QUEUE_NAME = "validate-order-result";
    public static final String ALLOCATE_ORDER_QUEUE_NAME = "allocate-order";
    public static final String ALLOCATE_ORDER_RESULT_QUEUE_NAME = "allocate-order-result";
    public static final String ALLOCATE_ORDER_FAILURE_QUEUE = "failure-allocation";
    public static final String DEALLOCATE_ORDER_QUEUE_NAME = "deallocate-order";

    @Bean
    public MessageConverter messageConverter() {

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(objectMapper);

        return converter;
    }

}
