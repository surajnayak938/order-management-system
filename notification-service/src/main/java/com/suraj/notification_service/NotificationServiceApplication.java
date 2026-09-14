package com.suraj.notification_service;

import com.suraj.notification_service.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication
@Slf4j
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @KafkaListener(topics = "notificationTopic")
    public void handleNotification(OrderPlacedEvent orderPlacedEvent){
        //Send required Notification Email or any other required notification
        log.info("Received Notification for Order - [{}]", orderPlacedEvent.getOrderNumber());
    }
}
