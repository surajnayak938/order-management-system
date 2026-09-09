package com.suraj.order_service.controller;

import com.suraj.order_service.dto.OrderRequest;
import com.suraj.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody OrderRequest orderRequest){
        log.info(orderRequest.toString());
        orderService.placeOrder(orderRequest);
        return "order Placed Successfully";
    }
}
