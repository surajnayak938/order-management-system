package com.suraj.order_service.service;

import com.suraj.order_service.dto.InventoryResponseDTO;
import com.suraj.order_service.dto.OrderLineItemsDto;
import com.suraj.order_service.dto.OrderRequest;
import com.suraj.order_service.event.OrderPlacedEvent;
import com.suraj.order_service.model.Order;
import com.suraj.order_service.model.OrderLineItems;
import com.suraj.order_service.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;
    private final Tracer tracer;
    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;

    public CompletableFuture<String> placeOrder(OrderRequest orderRequest){
        return CompletableFuture.supplyAsync(() -> placeOrderSynchronously(orderRequest));
    }

    private String placeOrderSynchronously(OrderRequest orderRequest) {
        Map<String, Long> requestedQuantities = new LinkedHashMap<>();
        if (orderRequest.getOrderLineItemsDtoList() == null || orderRequest.getOrderLineItemsDtoList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain items");
        }
        for (OrderLineItemsDto item : orderRequest.getOrderLineItemsDtoList()) {
            if (item == null || item.getSkuCode() == null || item.getSkuCode().isBlank()
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each item must have a SKU and a positive quantity");
            }
            requestedQuantities.merge(item.getSkuCode(), item.getQuantity().longValue(), Long::sum);
        }
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToDto).toList();
        order.setOrderLineItemsList(orderLineItemsList);

        List<String> skuCodes = List.copyOf(requestedQuantities.keySet());

        log.info("Calling inventory service");

        Span invenoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

        try(Tracer.SpanInScope spanInScope = tracer.withSpan(invenoryServiceLookup.start())){
            //call inventory Service, and place order only if product is available in inventory
            InventoryResponseDTO[] inventoryResponseDTOS = webClient.
                    get().
                    uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build()).
                    retrieve().
                    bodyToMono(InventoryResponseDTO[].class).
                    block();

            boolean allProductsInStock = !skuCodes.isEmpty() && inventoryResponseDTOS != null
                    && skuCodes.stream().allMatch(sku -> Arrays.stream(inventoryResponseDTOS)
                    .anyMatch(item -> item != null && sku.equals(item.getSkuCode()) && item.isInStock()
                            && item.getAvailableQuantity() != null
                            && item.getAvailableQuantity().longValue() >= requestedQuantities.get(sku)));
            if(allProductsInStock){
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order Placed Successfully!!";
            }else {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "One or more products are missing from inventory or have insufficient stock");
            }
        }finally {
            invenoryServiceLookup.end();
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
