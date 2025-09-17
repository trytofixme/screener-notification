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
        try {
            RsiNotificationDto dto = getEvent(record.value());
            sendNotification(dto.getTelegramId(),getMessage(dto))
                    .block(); // да, синхронный мост, но на ОДНО сообщение
            ack.acknowledge();
        } catch (Exception e) {
            // решите политику: nack/skip/DLT
            ack.nack(Duration.ofSeconds(5));
        }
    }
}
