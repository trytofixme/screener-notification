package ru.screener.notification.integrations.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.screener.notification.config.telegram.TelegramProperties;
import ru.screener.notification.service.NotificationService;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClient implements NotificationService {

    private static final String SEND_MESSAGE_METHOD = "sendMessage";

    private final RestTemplate restTemplate;
    private final TelegramProperties telegramProperties;

    private String url(String method) {
        return String.format("%s/bot%s/%s", telegramProperties.getApiBaseUrl(), telegramProperties.getBotToken(), method);
    }

    @Override
    public void send(Long userId, String message) {
        Map<String, String> body = Map.of(
                "chat_id", userId.toString(),
                "text", message
        );

        RequestEntity<Map<String, String>> request = RequestEntity
                .post(url(SEND_MESSAGE_METHOD))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        restTemplate.exchange(request, String.class);

        log.info("Telegram notification sent to user {}: {}", userId, message);
    }
}
