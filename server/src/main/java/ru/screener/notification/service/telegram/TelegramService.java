package ru.screener.notification.service.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.screener.notification.config.telegram.GlobalRateLimiter;
import ru.screener.notification.config.telegram.TelegramProperties;
import ru.screener.notification.config.telegram.TelegramRetryPolicy;
import ru.screener.notification.integrations.telegram.TelegramClient;
import ru.screener.notification.dto.telegram.TelegramSendRequestDto;
import ru.screener.notification.service.NotificationService;
import ru.screener.notification.utils.request.TelegramRequestBuilder;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService implements NotificationService {

    private final TelegramClient telegramClient;
    private final TelegramRetryPolicy retryPolicy;
    private final GlobalRateLimiter globalLimiter;
    private final TelegramProperties telegramProperties;

    private final ConcurrentHashMap<Long, AtomicLong> lastSend = new ConcurrentHashMap<>();

    public Mono<Void> sendAsync(Long chatId, String text) {
        TelegramSendRequestDto dto = TelegramRequestBuilder.buildPlain(chatId, text);

        long now = System.currentTimeMillis();
        var last = lastSend.computeIfAbsent(chatId, k -> new AtomicLong(0));
        long elapsed = now - last.get();
        long sleepMs = elapsed >= telegramProperties.getRatelimit().getPerChatMinIntervalMs()
                ? 0 : telegramProperties.getRatelimit().getPerChatMinIntervalMs() - elapsed;

        return Mono.delay(Duration.ofMillis(sleepMs))
                .then(globalLimiter.acquire())
                .then(telegramClient.sendMessage(dto))
                .retryWhen(retryPolicy.retry())
                .doOnSuccess(v -> last.set(System.currentTimeMillis()));
    }
}
