package ru.screener.notification.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.screener.notification.client.response.UserResponse;
import ru.screener.notification.service.NotificationService;

@Component
@Slf4j
public class TelegramService implements NotificationService {

    @Override
    public void send(UserResponse user, String message) {
        log.info("Telegram sent to user {} (phone: {}): {}", user.getUsername(), user.getPhoneNumber(), message);
    }
}
