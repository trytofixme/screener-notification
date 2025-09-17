package ru.screener.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.screener.notification.errors.kafka.InvalidKafkaMessageException;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.service.NotificationService;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractKafkaListener<T> {

    private final NotificationService notificationService;
    private final MessageBuilder<T> messageBuilder;
    private final ObjectMapper objectMapper;
    private final Class<T> eventType;

    public T getEvent(String message) {
        try {
            return objectMapper.readValue(message, eventType);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize Kafka message into class {}. Message: {}. Error: {}",
                    eventType.getSimpleName(), message, ex.getMessage(), ex);
            throw new InvalidKafkaMessageException("Invalid message format", ex);
        }
    }

    public String getMessage(T event) {
        return messageBuilder.buildMessage(event);
    }

    public void sendNotification(Long userId, String text) {
        notificationService.send(userId, text);
    }
}
