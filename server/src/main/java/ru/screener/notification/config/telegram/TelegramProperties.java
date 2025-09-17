package ru.screener.notification.config.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private String botToken;
    private String apiBaseUrl;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private int maxGlobalRps = 25;
}
