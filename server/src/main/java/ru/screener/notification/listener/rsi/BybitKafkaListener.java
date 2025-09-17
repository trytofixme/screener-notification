package ru.screener.notification.listener.rsi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.screener.notification.config.kafka.KafkaNotificationTopicProperties;
import ru.screener.notification.dto.rsi.RsiNotificationDto;
import ru.screener.notification.errors.telegram.RetryAfterException;
import ru.screener.notification.listener.AbstractKafkaListener;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.service.NotificationService;

import java.time.Duration;

@Component
@Slf4j
public class BybitKafkaListener extends AbstractKafkaListener<RsiNotificationDto> {

    private final KafkaNotificationTopicProperties topicProperties;

    public BybitKafkaListener(NotificationService notificationService,
                              MessageBuilder<RsiNotificationDto> messageBuilder,
                              KafkaNotificationTopicProperties topicProperties,
                              ObjectMapper objectMapper) {
        super(notificationService, messageBuilder, objectMapper, RsiNotificationDto.class);
        this.topicProperties = topicProperties;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.notification.name}",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenBybitTopic(String message, Acknowledgment ack) {
        RsiNotificationDto event = getEvent(message);
        log.info("Received a message from {}: {}", topicProperties.getName(), event);

        String text = getMessage(event);

        sendNotification(event.getTelegramId(), text)
                .timeout(Duration.ofSeconds(15)) // крайний дедлайн для всей операции
                .doOnSuccess(v -> {
                    ack.acknowledge();
                    log.debug("Sent to {} OK", event.getTelegramId());
                })
                .onErrorResume(ex -> {
                    ack.acknowledge();
                    return Mono.empty();
                })
                .subscribe(); // ошибок «наружу» больше нет — onErrorDropped не появится
    }
}
