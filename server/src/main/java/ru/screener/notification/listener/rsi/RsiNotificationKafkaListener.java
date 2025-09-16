package ru.screener.notification.listener.rsi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.screener.notification.dto.rsi.RsiNotificationDto;
import ru.screener.notification.errors.kafka.InvalidKafkaMessageException;
import ru.screener.notification.listener.AbstractKafkaListener;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.service.NotificationService;

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
        try {
            RsiNotificationDto rsiNotificationDto = getEvent(record.value());
            log.info("Received a message from {}: {}", record.topic(), rsiNotificationDto);

            String message = getMessage(rsiNotificationDto);
            sendNotification(rsiNotificationDto.getTelegramId(), message)
                    .doOnError(e -> log.warn("TG send failed for {}: {}", rsiNotificationDto.getTelegramId(), e.toString()))
                    .block();

            ack.acknowledge();
        } catch (InvalidKafkaMessageException ex) {
            log.error(ex.getMessage());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Error processing record: {}", record, ex);
            throw ex;
        }
    }
}
