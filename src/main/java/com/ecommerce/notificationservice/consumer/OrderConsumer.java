package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderConsumer {

    @RabbitListener(queues = "order.created.queue")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("==================================================");
        log.info(" BİLDİRİM SERVİSİ: Yeni Sipariş Bildirimi Yakalandı!");
        log.info(" Sipariş ID  : {}", event.getOrderId());
        log.info(" Müşteri Mail: {}", event.getUserEmail());
        log.info(" Toplam Tutar: {} TL", event.getTotalAmount());
        log.info(" E-posta simülasyonu: Müşteriye bildirim gönderildi.");
        log.info("==================================================");
    }
}