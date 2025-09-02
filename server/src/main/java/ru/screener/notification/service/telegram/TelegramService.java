package ru.screener.notification.service.telegram;

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
public class TelegramService implements NotificationService {

    private final RestTemplate restTemplate;
    private final TelegramProperties properties;

    @Override
    public void send(String userId, String message) {
        String url = "https://api.telegram.org/bot" + properties.getBotToken() + "/sendMessage";
        Map<String, String> body = Map.of(
                "chat_id", userId,
                "text", message
        );

        RequestEntity<Map<String, String>> request = RequestEntity
                .post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        restTemplate.exchange(request, String.class);

        log.info("Telegram sent to user {}: {}", userId, message);
    }
}
