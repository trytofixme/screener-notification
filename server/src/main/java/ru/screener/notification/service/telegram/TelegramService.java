package ru.screener.notification.service.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.screener.notification.integrations.telegram.TelegramClient;
import ru.screener.notification.service.NotificationService;
import ru.screener.notification.utils.request.TelegramRequestBuilder;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
@Service
@Slf4j
public class TelegramService implements NotificationService {

    private static final int PER_CHAT_MIN_INTERVAL_MS = 1000;

    private final TelegramClient telegramClient;
    private final ConcurrentHashMap<Long, AtomicLong> chatNextAt = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> send(Long userId, String message) {
        var dto = TelegramRequestBuilder.buildPlain(userId, message);

        Duration delay = perChatDelay(dto.chat_id());

        return Mono
                .delay(delay)
                .then(telegramClient.send(dto)
                        .retryWhen(telegramClient.telegramRetry()));
    }

    private Duration perChatDelay(long chatId) {
        long now = System.currentTimeMillis();
        AtomicLong nextAtRef = chatNextAt.computeIfAbsent(chatId, k -> new AtomicLong(0));

        long prev = nextAtRef.getAndUpdate(prevVal -> {
            long earliest = Math.max(prevVal, now);
            return earliest + PER_CHAT_MIN_INTERVAL_MS;
        });

        long delayMs = Math.max(0, prev - now);
        return Duration.ofMillis(delayMs);
    }
}
