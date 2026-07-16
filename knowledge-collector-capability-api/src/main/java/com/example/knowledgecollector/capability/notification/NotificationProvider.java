package com.example.knowledgecollector.capability.notification;

import java.util.Map;

public interface NotificationProvider {
    NotificationResult send(NotificationRequest request);

    record NotificationRequest(String channel, String recipient, String subject,
                               String content, Map<String, String> attributes) {
    }

    record NotificationResult(boolean success, String externalId, String message) {
    }
}
