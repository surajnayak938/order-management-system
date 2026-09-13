package com.suraj.order_service.controller;

import com.suraj.order_service.dto.OrderRequest;
import com.suraj.order_service.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name="inventory")
    @Retry(name="inventory")
    public CompletableFuture<String> placeOrder(@RequestBody OrderRequest orderRequest){
        log.info(orderRequest.toString());
        return orderService.placeOrder(orderRequest);
    }

    public CompletableFuture<String> fallbackMethod(OrderRequest orderRequest, Throwable throwable){
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof org.springframework.web.server.ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            cause = cause.getCause();
        }
        return CompletableFuture.supplyAsync(()->"Oops! Something went wrong, Please order after some time!");
    }
}
