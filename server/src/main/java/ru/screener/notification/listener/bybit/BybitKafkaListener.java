package ru.screener.notification.listener.bybit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.screener.notification.config.kafka.KafkaBybitTopicProperties;
import ru.screener.notification.listener.AbstractKafkaListener;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.model.bybit.BybitEvent;
import ru.screener.notification.service.NotificationService;

@Component
@Slf4j
public class BybitKafkaListener extends AbstractKafkaListener<BybitEvent> {

    private final KafkaBybitTopicProperties topicProperties;

    public BybitKafkaListener(NotificationService notificationService,
                              MessageBuilder<BybitEvent> messageBuilder,
                              KafkaBybitTopicProperties topicProperties,
                              ObjectMapper objectMapper) {
        super(notificationService, messageBuilder, objectMapper, BybitEvent.class);
        this.topicProperties = topicProperties;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.notification.name}",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenBybitTopic(String message, Acknowledgment ack) {
        BybitEvent bybitEvent = getEvent(message);
        log.info("Received a message from {}: {}", topicProperties.getName(), bybitEvent);

        String messageText = getMessage(bybitEvent);

        sendNotification(bybitEvent.getTelegramId(), messageText)
                .doOnError(e -> log.warn("TG send failed for {}: {}", bybitEvent.getTelegramId(), e.toString()))
                .subscribe();

        ack.acknowledge();
    }
}
