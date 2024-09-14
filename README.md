# Notification Service

This `Notification Service` is a Spring Boot microservice that handles sending various email notifications, such as product updates, low stock alerts, and user sign-up welcome emails, using **JavaMailSender** for email delivery and **Kafka** for event-driven communication between microservices.

## Features

- **Email Notifications**:
  - Send product update notifications.
  - Send welcome emails to users on sign-up.
  - Send low stock alerts to admins.
  
- **Event-Driven Architecture**:
  - Integrated with Apache Kafka to listen for different events (product updates, user sign-up, and low stock alerts) and trigger email notifications.

## Table of Contents

- [Dependencies](#dependencies)
- [Kafka Topics](#kafka-topics)
- [Configuration](#configuration)
- [Usage](#usage)
- [Scheduler for Low Stock Alerts](#scheduler-for-low-stock-alerts)

---

## Dependencies

This project uses the following dependencies in the `pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Mail -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>

    <!-- Kafka Integration -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Jackson for JSON processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

## Kafka Topics

This service listens to three Kafka topics:
1. **product_updates** - Sends product update notifications to admins.
2. **signUp_Update_toUser** - Sends a welcome email to newly registered users.
3. **low_stock_alerts** - Sends low stock alerts to admins.

### Sample Kafka Configuration in `application.properties`

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification_group
spring.kafka.consumer.auto-offset-reset=earliest
```

## Configuration

To enable email sending, you must configure the following properties in the `application.properties` file for JavaMailSender to connect to the mail server (using Gmail as an example):

```properties
server.port=8004
spring.application.name=notificationservice

# Email configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-email-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Usage

### Sending Emails

The `NotificationService` is responsible for sending emails using the JavaMailSender. Here's how the service is used:

```java
@Service
public class NotificationService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
```

### Listening to Kafka Events

The `NotificationEventListener` class listens for various Kafka topics and sends corresponding emails when a message is received:

```java
@Service
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;

    // Listen for product updates
    @KafkaListener(topics = "product_updates", groupId = "notification_group")
    public void listenProductUpdates(String message) {
        notificationService.sendEmail("admin@example.com", "Product Update", message);
    }

    // Listen for sign-up event to send welcome email
    @KafkaListener(topics = "signUp_Update_toUser", groupId = "notification_group")
    public void sendSignUpEmail(JsonNode node) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.treeToValue(node, User.class);
            notificationService.sendEmail(user.getEmail(), "Sign Up Email", "Hi user, welcome to our Ecommerce App");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    // Listen for low stock alerts
    @KafkaListener(topics = "low_stock_alerts", groupId = "notification_group")
    public void listenLowStockAlerts(String message) {
        notificationService.sendEmail("admin@example.com", "Low Stock Alert", message);
    }
}
```

### Scheduler for Low Stock Alerts

In case of low stock, a scheduler runs every hour to check the stock levels and update inventory. You can collect low-stock product information and send a cumulative email to the admin at the end of each scheduler run.

Example of the scheduler:

```java
@Scheduled(cron = "0 0 * * * *")
@Transactional
public void updateLowStockInventory() {
    List<BelowThresholdProductQuantity> lowStockProducts = belowThresholdProductsRepository.findAll();
    if (!lowStockProducts.isEmpty()) {
        List<String> lowStockProductIds = new ArrayList<>();
        for (BelowThresholdProductQuantity lowStock : lowStockProducts) {
            // Fetch product, update inventory, and collect low stock products
            lowStockProductIds.add(lowStock.getProductId());
        }
        belowThresholdProductsRepository.deleteAll(lowStockProducts);

        // Send cumulative email to admin
        if (!lowStockProductIds.isEmpty()) {
            sendLowStockAlertEmail(lowStockProductIds);
        }
    }
}

private void sendLowStockAlertEmail(List<String> lowStockProductIds) {
    String message = "The following products are low on stock: \n" + String.join(", ", lowStockProductIds);
    notificationService.sendEmail("admin@example.com", "Low Stock Alert", message);
}
```

## Conclusion

This `Notification Service` ensures efficient and event-driven email communication in your microservice architecture. It listens to events from other services via Kafka and sends emails to users or administrators based on those events. The scheduler ensures that stock levels are updated regularly, and cumulative email reports help reduce email clutter.
