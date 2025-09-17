package ru.screener.notification.service.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.screener.notification.config.telegram.GlobalRateLimiter;
import ru.screener.notification.config.telegram.PerChatLimiter;
import ru.screener.notification.config.telegram.TelegramRetryPolicy;
import ru.screener.notification.dto.telegram.TelegramSendRequestDto;
import ru.screener.notification.integrations.telegram.TelegramClient;
import ru.screener.notification.service.NotificationService;
import ru.screener.notification.utils.request.TelegramRequestBuilder;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService implements NotificationService {

    private final TelegramClient telegramClient;
    private final TelegramRetryPolicy retryPolicy;
    private final GlobalRateLimiter globalLimiter;
    private final PerChatLimiter perChatLimiter;

    @Override
    public Mono<Void> sendAsync(Long chatId, String text) {
        TelegramSendRequestDto dto = TelegramRequestBuilder.buildPlain(chatId, text);

        return perChatLimiter.acquire(chatId)
                .then(globalLimiter.acquire())
                .then(telegramClient.sendMessage(dto)
                .timeout(Duration.ofSeconds(10))
                .retryWhen(retryPolicy.retry())
        );
    }
}
