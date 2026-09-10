package com.suraj.order_service;

import com.suraj.order_service.model.Order;
import com.suraj.order_service.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderServiceApplicationTests {

    @TestBean(methodName = "inventoryWebClient")
    private WebClient webClient;

    // Replace only the HTTP transport: retain real request construction and JSON decoding.
    static WebClient inventoryWebClient() {
        return WebClient.builder().exchangeFunction(request -> {
            assertThat(request.method()).isEqualTo(HttpMethod.GET);
            assertThat(request.url().getPath()).isEqualTo("/api/inventory");
            assertThat(UriComponentsBuilder.fromUri(request.url()).build()
                    .getQueryParams().get("skuCode"))
                    .containsExactly("i_Phone_13", "phone_case");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            [{"skuCode":"i_Phone_13","inStock":true},
                             {"skuCode":"phone_case","inStock":true}]
                            """)
                    .build());
        }).build();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    private static final String VALID_ORDER = """
            {
              "orderLineItemsDtoList": [
                {"id": 999, "skuCode": "i_Phone_13", "price": 1200.50, "quantity": 1},
                {"skuCode": "phone_case", "price": 19.99, "quantity": 2}
              ]
            }
            """;

    @Test
    void postOrderReturnsCreatedAndPersistsAllLineItems() throws Exception {
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(content().string("order Placed Successfully"));

        // Force SQL execution and reload, rather than inspecting cached entities.
        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findAll()).hasSize(1);
        Order savedOrder = orderRepository.findAll().get(0);
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(UUID.fromString(savedOrder.getOrderNumber()).toString())
                .isEqualTo(savedOrder.getOrderNumber());
        assertThat(savedOrder.getOrderLineItemsList()).hasSize(2);
        assertThat(savedOrder.getOrderLineItemsList()).allSatisfy(item -> {
            assertThat(item.getId()).isNotNull().isNotEqualTo(999L);
        });
        assertThat(savedOrder.getOrderLineItemsList()).anySatisfy(item -> {
            assertThat(item.getSkuCode()).isEqualTo("i_Phone_13");
            assertThat(item.getPrice()).isEqualByComparingTo("1200.50");
            assertThat(item.getQuantity()).isEqualTo(1);
        });
        assertThat(savedOrder.getOrderLineItemsList()).anySatisfy(item -> {
            assertThat(item.getSkuCode()).isEqualTo("phone_case");
            assertThat(item.getPrice()).isEqualByComparingTo("19.99");
            assertThat(item.getQuantity()).isEqualTo(2);
        });
    }

    @Test
    void separateOrdersReceiveDifferentOrderNumbers() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_ORDER))
                    .andExpect(status().isCreated());
        }
        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findAll())
                .hasSize(2)
                .extracting(Order::getOrderNumber)
                .doesNotHaveDuplicates();
    }

    @Test
    void malformedJsonReturnsBadRequestWithoutSavingOrder() throws Exception {
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderLineItemsDtoList\": ["))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void missingRequestBodyReturnsBadRequestWithoutSavingOrder() throws Exception {
        mockMvc.perform(post("/api/order").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void nonNumericQuantityReturnsBadRequestWithoutSavingOrder() throws Exception {
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderLineItemsDtoList": [
                                  {"skuCode": "phone", "price": 1200, "quantity": "invalid"}
                                ]}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void plainTextBodyReturnsUnsupportedMediaTypeWithoutSavingOrder() throws Exception {
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(VALID_ORDER))
                .andExpect(status().isUnsupportedMediaType());

        assertThat(orderRepository.count()).isZero();
    }
}
