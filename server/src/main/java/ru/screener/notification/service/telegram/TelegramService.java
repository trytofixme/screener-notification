package ru.screener.notification.service.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.screener.notification.config.telegram.TelegramProperties;
import ru.screener.notification.dto.telegram.TelegramSendRequestDto;
import ru.screener.notification.errors.telegram.PermanentTelegramException;
import ru.screener.notification.errors.telegram.RetryAfterException;
import ru.screener.notification.errors.telegram.TooManyRequestsException;
import ru.screener.notification.errors.telegram.TransientTelegramException;
import ru.screener.notification.integrations.telegram.TelegramClient;
import ru.screener.notification.service.NotificationService;
import ru.screener.notification.utils.request.TelegramRequestBuilder;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService implements NotificationService {

    private static final int PER_CHAT_MIN_INTERVAL_MS = 1000;

    private final TelegramClient telegramClient;

    private final ConcurrentHashMap<Long, AtomicLong> chatLastSend = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> send(Long userId, String message) {
        TelegramSendRequestDto dto = TelegramRequestBuilder.buildPlain(userId, message);
        long now = System.currentTimeMillis();
        AtomicLong last = chatLastSend.computeIfAbsent(dto.chat_id(), k -> new AtomicLong(0));
        long elapsed = now - last.get();
        long sleepMs = elapsed >= PER_CHAT_MIN_INTERVAL_MS ? 0 : PER_CHAT_MIN_INTERVAL_MS - elapsed;

        return Mono.delay(Duration.ofMillis(sleepMs))
                .then(sendOnce(dto))
                .doOnSuccess(v -> last.set(System.currentTimeMillis()));
    }

    private Mono<Void> sendOnce(TelegramSendRequestDto dto) {
        return telegramClient.doPost(dto)
                .retryWhen(telegramClient.telegramRetry());
    }
}
