package ru.screener.notification.listener.rsi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.screener.notification.config.kafka.KafkaNotificationTopicProperties;
import ru.screener.notification.dto.rsi.RsiNotificationDto;
import ru.screener.notification.errors.kafka.InvalidKafkaMessageException;
import ru.screener.notification.errors.telegram.TooManyRequestsException;
import ru.screener.notification.errors.telegram.TransientTelegramException;
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
        RsiNotificationDto bybitEvent = getEvent(message);
        log.info("Received a message from {}: {}", topicProperties.getName(), bybitEvent);

        String messageText = getMessage(bybitEvent);

        sendNotification(bybitEvent.getTelegramId(), messageText)
                .timeout(Duration.ofSeconds(15))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> ack.acknowledge())
                .doOnError(ex -> {
                    if (ex instanceof TooManyRequestsException || ex instanceof TransientTelegramException) {
                        ack.nack(Duration.ofSeconds(10));
                    } else {
                        log.error("Permanent error, send to DLT", ex);
                        ack.acknowledge();
                    }
                })
                .subscribe(
                        null,
                        ex -> log.warn("Send failed, cause {}", ex.getMessage())
                );
    }
}
