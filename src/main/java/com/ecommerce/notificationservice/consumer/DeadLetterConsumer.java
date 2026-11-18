package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class DeadLetterConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_DLQ)
    public void handleDeadLetterMessage(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        log.error("[DLQ ALERT] İşlenemeyen mesaj Dead Letter Queue'ya düştü! x-death={} | Payload={}", xDeath, payload);
    }
}