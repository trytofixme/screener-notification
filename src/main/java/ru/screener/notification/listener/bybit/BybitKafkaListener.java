package ru.screener.notification.listener.bybit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.screener.notification.client.UserServiceClient;
import ru.screener.notification.client.response.UserResponse;
import ru.screener.notification.config.kafka.KafkaBybitTopicProperties;
import ru.screener.notification.listener.AbstractKafkaListener;
import ru.screener.notification.messaging.MessageBuilder;
import ru.screener.notification.model.kafka.bybit.BybitEvent;
import ru.screener.notification.model.kafka.bybit.BybitMessage;
import ru.screener.notification.service.NotificationService;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
public class BybitKafkaListener extends AbstractKafkaListener<BybitEvent, BybitMessage> {

    private final UserServiceClient userServiceClient;
    private final KafkaBybitTopicProperties topicProperties;

    public BybitKafkaListener(NotificationService notificationService,
                              List<MessageBuilder<BybitMessage>> messageBuilders,
                              UserServiceClient userServiceClient,
                              KafkaBybitTopicProperties topicProperties,
                              ObjectMapper objectMapper) {
        super(notificationService, messageBuilders, objectMapper,
                BybitEvent.class, BybitMessage.class);
        this.userServiceClient = userServiceClient;
        this.topicProperties = topicProperties;
    }

    @KafkaListener(
            topics = "${spring.data.kafka.topic.notification.name}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenBybitTopic(String message) {
        BybitEvent bybitEvent = getEvent(message);
        log.info("Received a message from {}: {}", topicProperties.getName(), bybitEvent);
        BybitEvent.EventInfo eventInfo = bybitEvent.getEventInfo();

        BybitMessage bybitMessage = getBybitMessage(eventInfo.getSymbol(), eventInfo.getStrategyName(), eventInfo.getValue());
        UserResponse userResponse = userServiceClient.getUserById(bybitEvent.getTelegramId());

        String text = getMessage(bybitMessage, userResponse.getLocale());

        sendNotification(userResponse, text);
    }

    private BybitMessage getBybitMessage(String symbol, String strategyName, BigDecimal value) {
        return new BybitMessage()
                .setSymbol(symbol)
                .setStrategyName(strategyName)
                .setValue(value);
    }
}
