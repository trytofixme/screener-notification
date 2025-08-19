package ru.screener.notification.service;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import ru.screener.notification.config.TelegramProperties;
import ru.screener.notification.model.BybitEvent;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final RestTemplate restTemplate;
    private final TelegramProperties properties;
    private final MessageFormatter formatter;

    public void send(BybitEvent event) {
        String message = formatter.format(event);
        String url = "https://api.telegram.org/bot" + properties.getBotToken() + "/sendMessage";
        Map<String, String> body = Map.of(
                "chat_id", properties.getChatId(),
                "text", message
        );
        RequestEntity<Map<String, String>> request = RequestEntity
                .post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        restTemplate.exchange(request, String.class);
    }
}
