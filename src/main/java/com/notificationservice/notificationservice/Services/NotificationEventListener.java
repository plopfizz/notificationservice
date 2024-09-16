package com.notificationservice.notificationservice.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.notificationservice.Dtos.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "product_updates", groupId = "notification_group")
    public void listenProductUpdates(String message) {
        notificationService.sendEmail("admin@example.com", "Product Update", message);
    }
    @KafkaListener(topics = "signUp_Update_toUser", groupId = "notification_group")
    public void sendSignUpEmail(JsonNode node){
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            User user = objectMapper.treeToValue(node, User.class);
            notificationService.sendEmail(user.getEmail(), "Sign Up Email", "Hi user, welcome to our Ecommerce App");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "low_stock_alerts", groupId = "notification_group")
    public void listenLowStockAlerts(JsonNode jsonNode) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(jsonNode);

        System.out.println("we are in  low stock alerts");
        notificationService.sendEmail("mmandlecha@gmail.com", "Low Stock Alert", message);
    }
}
