package ru.screener.notification.config.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private String botToken;
    private String chatId;
}
