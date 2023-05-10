package guru.sfg.beer.order.service.config;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("localdiscovery")
@EnableDiscoveryClient
@Configuration
public class LocalDiscovery {
}
