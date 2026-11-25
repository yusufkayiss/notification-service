package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.config.RabbitMQConfig;
import com.ecommerce.notificationservice.event.OrderCreatedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderConsumer {

    private final Tracer tracer;

    public OrderConsumer() {
        this(null);
    }

    @Autowired
    public OrderConsumer(Tracer tracer) {
        this.tracer = tracer;
    }

    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        consumeOrderCreatedEvent(event, null);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    public void consumeOrderCreatedEvent(
            @Payload OrderCreatedEvent event,
            @Header(name = "traceparent", required = false) String traceparent) {

        Span consumerSpan = startConsumerSpan(traceparent);

        try (Tracer.SpanInScope ignored = (consumerSpan != null ? tracer.withSpan(consumerSpan) : null)) {
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
        } catch (Exception ex) {
            if (consumerSpan != null) {
                consumerSpan.error(ex);
            }
            throw ex;
        } finally {
            if (consumerSpan != null) {
                consumerSpan.end();
            }
        }
    }

    private Span startConsumerSpan(String traceparent) {
        if (tracer == null) {
            return null;
        }

        TraceContext parentContext = parseW3CTraceParent(traceparent);
        Span.Builder spanBuilder = (parentContext != null)
                ? tracer.spanBuilder().setParent(parentContext)
                : tracer.spanBuilder();

        return spanBuilder
                .name("consume " + RabbitMQConfig.QUEUE_ORDER_CREATED)
                .kind(Span.Kind.CONSUMER)
                .tag("messaging.system", "rabbitmq")
                .tag("messaging.destination", RabbitMQConfig.QUEUE_ORDER_CREATED)
                .start();
    }

    private TraceContext parseW3CTraceParent(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return null;
        }
        String[] parts = traceparent.trim().split("-");
        if (parts.length < 4 || parts[1].length() != 32 || parts[2].length() != 16) {
            return null;
        }
        boolean sampled = (Integer.parseInt(parts[3], 16) & 0x01) == 1;
        return tracer.traceContextBuilder()
                .traceId(parts[1])
                .spanId(parts[2])
                .sampled(sampled)
                .build();
    }
}