# 🔔 Notification Service

This microservice listens for order events from RabbitMQ and processes simulated notification workflows (e.g., sending emails).

## 🚀 Tech Stack
- **Java 21**
- **Spring Boot 3.x**
- **Spring AMQP (RabbitMQ)**
- **Maven**

## 🏗️ Architecture & Flow
1. `ecommerce-core-api` creates an order and publishes `OrderCreatedEvent` to RabbitMQ.
2. `notification-service` consumes messages from `order.created.queue`.
3. Notification process is executed and logged.

## 🛠️ Configuration
Ensure RabbitMQ is running on `localhost:5672` (or configured via environment variables).

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

## ⚙️ Running Locally
```bash
./mvnw spring-boot:run
```
