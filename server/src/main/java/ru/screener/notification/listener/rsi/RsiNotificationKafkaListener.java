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
import ru.screener.notification.dto.rsi.RsiNotificationDto;
import ru.screener.notification.errors.kafka.InvalidKafkaMessageException;
import ru.screener.notification.listener.AbstractKafkaListener;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.service.NotificationService;

@Component
@Slf4j
public class RsiNotificationKafkaListener extends AbstractKafkaListener<RsiNotificationDto> {

    private final KafkaListenerEndpointRegistry registry;

    public RsiNotificationKafkaListener(NotificationService notificationService,
                                        KafkaListenerEndpointRegistry registry,
                                        MessageBuilder<RsiNotificationDto> messageBuilder,
                                        ObjectMapper objectMapper) {
        super(notificationService, messageBuilder, objectMapper, RsiNotificationDto.class);
        this.registry = registry;
    }

    @KafkaListener(id = "rsiListenerContainer", topics = "${app.kafka.topic.notification.name}", containerFactory = "kafkaListenerContainerFactory")
    public void listenBybitTopic(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Get message {}", record.value());
        MessageListenerContainer container = registry.getListenerContainer("rsiListenerContainer");
        if (container == null) {
            log.error("Cannot find listener container with id 'rsiListenerContainer'");
            ack.acknowledge();
            return;
        }
        container.pause();

        final RsiNotificationDto rsiNotificationDto;
        try {
            rsiNotificationDto = getEvent(record.value());
            log.info("Received a message from {}: {}", record.topic(), rsiNotificationDto);
        } catch (InvalidKafkaMessageException ex) {
            log.error("Invalid message, skip: {}", ex.getMessage());
            ack.acknowledge();
            container.resume();
            return;
        }

        final String message = getMessage(rsiNotificationDto);
        sendNotification(rsiNotificationDto.getTelegramId(), message)
                .doOnSuccess(v -> log.info("Sent to TG: {}", rsiNotificationDto.getTelegramId()))
                .doOnError(e -> log.warn("TG send failed after retries for {}: {}", rsiNotificationDto.getTelegramId(), e.toString()))
                .doFinally(signalType -> {
                    log.info("Acknowledging message and resuming container. Signal: {}", signalType);
                    ack.acknowledge();
                    container.resume();
                })
                .subscribe();
    }
}
