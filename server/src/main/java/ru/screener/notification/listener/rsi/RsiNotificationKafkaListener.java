package ru.screener.notification.listener.rsi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;
import ru.screener.notification.dto.rsi.RsiNotificationDto;
import ru.screener.notification.errors.kafka.InvalidKafkaMessageException;
import ru.screener.notification.errors.telegram.TransientTelegramException;
import ru.screener.notification.listener.AbstractKafkaListener;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.service.NotificationService;

import java.time.Duration;

@Component
@Slf4j
public class RsiNotificationKafkaListener extends AbstractKafkaListener<RsiNotificationDto> {

    public RsiNotificationKafkaListener(NotificationService notificationService,
                                        MessageBuilder<RsiNotificationDto> messageBuilder,
                                        ObjectMapper objectMapper) {
        super(notificationService, messageBuilder, objectMapper, RsiNotificationDto.class);
    }

    @KafkaListener(topics = "${app.kafka.topic.notification.name}", containerFactory = "kafkaListenerContainerFactory")
    public void listenBybitTopic(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Get message {}", record.value());

        final RsiNotificationDto rsiNotificationDto;
        try {
            rsiNotificationDto = getEvent(record.value());
            log.info("Received a message from {}: {}", record.topic(), rsiNotificationDto);
        } catch (InvalidKafkaMessageException ex) {
            log.error("Invalid message, skip: {}", ex.getMessage());
            ack.acknowledge();
            return;
        }

        final String message = getMessage(rsiNotificationDto);
        sendNotification(rsiNotificationDto.getTelegramId(), message)
                .doOnSuccess(v -> {
                    log.info("Sent to TG: {}", rsiNotificationDto.getTelegramId());
                    ack.acknowledge();
                })
                .doOnError(e -> {
                    log.warn("TG send failed after client retries for {}: {}", rsiNotificationDto.getTelegramId(), e.toString());
                    ack.acknowledge();
                })
                .subscribe();
    }
}
