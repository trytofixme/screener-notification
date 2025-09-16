package ru.screener.notification.config.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private String botToken;
    private String apiBaseUrl;
    private Methods methods = new Methods();
    private Timeouts timeouts = new Timeouts();
    private RateLimit ratelimit = new RateLimit();
    private RetryProps retry = new RetryProps();
    private Http http = new Http();

    @Data
    public static class Methods {

        private String sendMessage = "/sendMessage";
    }

    @Data public static class Timeouts {

        private int connectMs = 3000;
        private int readMs = 5000;
    }

    @Data public static class RateLimit {

        private int perChatMinIntervalMs = 1000;
        private int maxGlobalRps = 25;
    }

    @Data public static class RetryProps {

        private int tooManyRequestsMax = 3;
        private int transientMax = 5;
        private int baseMs = 300;
        private int base429Ms = 500;
        private int maxMs = 5000;
        private double jitterLow = 0.1;
        private double jitterHigh = 0.2;
    }

    @Data public static class Http {

        private int maxInMemoryBytes = 1024 * 1024;
    }
}
