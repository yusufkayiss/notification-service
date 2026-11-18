package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.config.RabbitMQConfig;
import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null || event.getUserEmail() == null || event.getUserEmail().isBlank()) {
            log.error("Geçersiz OrderCreatedEvent alındı, mesaj retry sonrası DLQ'ya yönlendirilecek: {}", event);
            throw new IllegalArgumentException("OrderCreatedEvent zorunlu alanları eksik (orderId veya userEmail)");
        }

        log.info("==================================================");
        log.info(" BİLDİRİM SERVİSİ: Yeni Sipariş Bildirimi Yakalandı!");
        log.info(" Sipariş ID  : {}", event.getOrderId());
        log.info(" Müşteri Mail: {}", event.getUserEmail());
        log.info(" Toplam Tutar: {} TL", event.getTotalAmount());
        log.info(" E-posta simülasyonu: Müşteriye bildirim gönderildi.");
        log.info("==================================================");
    }
}