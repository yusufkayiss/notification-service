package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderConsumerDlqTest {

    private final OrderConsumer orderConsumer = new OrderConsumer();
    private final DeadLetterConsumer deadLetterConsumer = new DeadLetterConsumer();

    @Test
    void shouldProcessValidOrderCreatedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                101L,
                "musteri@example.com",
                new BigDecimal("250.00"),
                LocalDateTime.now()
        );

        assertDoesNotThrow(() -> orderConsumer.consumeOrderCreatedEvent(event));
    }

    @Test
    void shouldThrowExceptionForInvalidEventToTriggerRetryAndDlq() {
        OrderCreatedEvent invalidEvent = new OrderCreatedEvent(
                102L,
                "",
                new BigDecimal("250.00"),
                LocalDateTime.now()
        );

        assertThrows(IllegalArgumentException.class, () -> orderConsumer.consumeOrderCreatedEvent(invalidEvent));
    }

    @Test
    void shouldConsumeDeadLetterMessageWithoutThrowing() {
        MessageProperties props = new MessageProperties();
        props.setHeader("x-death", List.of(Map.of("reason", "rejected", "count", 3L)));
        Message message = new Message("{\"orderId\":102}".getBytes(StandardCharsets.UTF_8), props);

        assertDoesNotThrow(() -> deadLetterConsumer.handleDeadLetterMessage(message));
    }
}