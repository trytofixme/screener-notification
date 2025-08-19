package ru.screener.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.screener.notification.exception.kafka.InvalidKafkaMessageException;
import ru.screener.notification.exception.messaging.MessageBuilderNotFoundException;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.service.NotificationService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractKafkaListener<T, R> {

    private final NotificationService notificationService;
    private final List<MessageBuilder<R>> messageBuilders;
    private final ObjectMapper objectMapper;
    private final Class<T> eventType;
    private final Class<R> messageType;

    public T getEvent(String message) {
        try {
            return objectMapper.readValue(message, eventType);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize Kafka message into class {}. Message: {}. Error: {}",
                    eventType.getSimpleName(), message, ex.getMessage(), ex);
            throw new InvalidKafkaMessageException("Invalid message format", ex);
        }
    }

    public String getMessage(R event) {
        return messageBuilders.stream()
                .filter(builder -> builder.getInstance() == messageType)
                .findAny()
                .orElseThrow(() -> {
                    log.warn("No MessageBuilder found for model class {}. Event: {}",
                            messageType.getSimpleName(), event);
                    return new MessageBuilderNotFoundException(messageType.getSimpleName());
                })
                .buildMessage(event);
    }

    public void sendNotification(String telegramId, String text) {
        notificationService.send(telegramId, text);
    }
}
