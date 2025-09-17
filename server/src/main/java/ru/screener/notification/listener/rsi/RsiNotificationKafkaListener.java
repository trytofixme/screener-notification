package ru.screener.notification.listener.rsi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.screener.notification.config.telegram.GlobalRateLimiter;
import ru.screener.notification.config.telegram.PerChatLimiter;
import ru.screener.notification.config.telegram.TelegramRetryPolicy;
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

    private final GlobalRateLimiter globalRateLimiter;
    private final PerChatLimiter perChatLimiter;
    private final TelegramRetryPolicy retryPolicy;

    public RsiNotificationKafkaListener(NotificationService notificationService,
                                        GlobalRateLimiter globalRateLimiter,
                                        PerChatLimiter perChatLimiter,
                                        TelegramRetryPolicy retryPolicy,
                                        MessageBuilder<RsiNotificationDto> messageBuilder,
                                        ObjectMapper objectMapper) {
        super(notificationService, messageBuilder, objectMapper, RsiNotificationDto.class);
        this.globalRateLimiter = globalRateLimiter;
        this.perChatLimiter = perChatLimiter;
        this.retryPolicy = retryPolicy;
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
        globalRateLimiter.acquire()
                .then(perChatLimiter.acquire(rsiNotificationDto.getTelegramId()))
                .then(sendNotification(rsiNotificationDto.getTelegramId(), message))
                .timeout(Duration.ofSeconds(5))
                .retryWhen(retryPolicy.retry())
                .doOnSuccess(v -> {
                    log.info("Sent to TG: {}", rsiNotificationDto.getTelegramId());
                    ack.acknowledge();
                })
                .onErrorResume(e -> {
                    log.warn("TG send failed after client retries for {}: {}", rsiNotificationDto.getTelegramId(), e.toString());
                    ack.acknowledge();
                    return Mono.empty();
                })
                .subscribe();
    }
}
