package ru.screener.notification.service.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
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

    private static final int PER_CHAT_INTERVAL_MS = 1000;

    private final TelegramClient client;

    private final ConcurrentHashMap<Long, AtomicLong> lastSend = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Scheduler> chatSchedulers = new ConcurrentHashMap<>();

    private Scheduler schedulerFor(long chatId) {
        return chatSchedulers.computeIfAbsent(chatId, id -> Schedulers.newSingle("tg-chat-" + id));
    }

    @Override
    public Mono<Void> send(Long userId, String message) {
        TelegramSendRequestDto dto = TelegramRequestBuilder.buildPlain(userId, message);

        return Mono.defer(() -> {
                    long now = System.currentTimeMillis();
                    AtomicLong last = lastSend.computeIfAbsent(dto.chat_id(), k -> new AtomicLong(0));
                    long elapsed = now - last.get();
                    long delay = elapsed >= PER_CHAT_INTERVAL_MS ? 0 : PER_CHAT_INTERVAL_MS - elapsed;
                    return Mono.delay(Duration.ofMillis(delay));
                })
                .publishOn(schedulerFor(dto.chat_id()))  // критично: последовательность по чату
                .then(client.send(dto)
                        .retryWhen(client.telegramRetry())
                )
                .doOnSuccess(v -> lastSend.get(dto.chat_id()).set(System.currentTimeMillis()));
    }
}

