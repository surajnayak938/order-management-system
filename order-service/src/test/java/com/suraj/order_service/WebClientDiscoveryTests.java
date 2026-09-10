package com.suraj.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.discovery.client.simple.instances.inventory-service[0].uri=http://127.0.0.1:18082"
})
@ActiveProfiles("test")
class WebClientDiscoveryTests {
    @Autowired
    @LoadBalanced
    private WebClient.Builder builder;

    @Test
    void resolvesServiceNameToDiscoveredHostAndPort() {
        AtomicReference<URI> destination = new AtomicReference<>();
        WebClient client = builder.clone().exchangeFunction(request -> {
            destination.set(request.url());
            return Mono.just(ClientResponse.create(HttpStatus.OK).body("ok").build());
        }).build();

        String response = client.get()
                .uri("http://inventory-service/api/inventory?skuCode=iphone_13")
                .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(5));

        assertThat(response).isEqualTo("ok");
        assertThat(destination.get()).isEqualTo(
                URI.create("http://127.0.0.1:18082/api/inventory?skuCode=iphone_13"));
    }
}
