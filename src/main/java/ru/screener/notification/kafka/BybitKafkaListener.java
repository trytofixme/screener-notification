package ru.screener.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.screener.notification.model.BybitEvent;
import ru.screener.notification.service.TelegramService;

@Component
@RequiredArgsConstructor
@Slf4j
public class BybitKafkaListener {

    private final ObjectMapper objectMapper;
    private final TelegramService telegramService;

    @KafkaListener(topics = "${spring.kafka.topic.notification.name}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            BybitEvent event = objectMapper.readValue(record.value(), BybitEvent.class);
            telegramService.send(event);
        } catch (Exception e) {
            log.error("Failed to process message {}", record.value(), e);
        }
    }
}
