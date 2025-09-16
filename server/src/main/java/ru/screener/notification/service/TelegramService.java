package ru.screener.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.screener.notification.config.telegram.TelegramProperties;
import ru.screener.notification.errors.telegram.PermanentTelegramException;
import ru.screener.notification.errors.telegram.RetryAfterException;
import ru.screener.notification.errors.telegram.TooManyRequestsException;
import ru.screener.notification.errors.telegram.TransientTelegramException;
import ru.screener.notification.integrations.telegram.TelegramClient;
import ru.screener.notification.model.dto.TelegramSendRequestDto;
import ru.screener.notification.utils.request.TelegramRequestBuilder;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService implements NotificationService {

    private static final int PER_CHAT_MIN_INTERVAL_MS = 1000;

    private final TelegramClient telegramClient;
    private final TelegramProperties telegramProperties;

    private final Duration requestTimeout = Duration.ofMillis(telegramProperties.getReadTimeoutMs());
    private final ConcurrentHashMap<Long, AtomicLong> chatLastSend = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> sendAsync(Long chatId, String text) {
        long now = System.currentTimeMillis();
        TelegramSendRequestDto dto = TelegramRequestBuilder.buildPlain(chatId, text);
        AtomicLong last = chatLastSend.computeIfAbsent(dto.chat_id(), k -> new AtomicLong(0));
        long elapsed = now - last.get();
        long sleepMs = elapsed >= PER_CHAT_MIN_INTERVAL_MS ? 0 : PER_CHAT_MIN_INTERVAL_MS - elapsed;

        return Mono.delay(Duration.ofMillis(sleepMs))
                .then(sendOnce(dto))
                .doOnSuccess(v -> last.set(System.currentTimeMillis()))
                .then();
    }

    private Mono<Void> sendOnce(TelegramSendRequestDto dto) {
        return telegramClient.doPost(dto)
                .timeout(requestTimeout)
                .flatMap(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) return Mono.empty();

                    if (resp.statusCode().value() == 429) {
                        return resp.headers().asHttpHeaders().containsKey("Retry-After")
                                ? Mono.error(new RetryAfterException(resp))
                                : Mono.error(new TooManyRequestsException());
                    }
                    if (resp.statusCode().is5xxServerError()) {
                        return Mono.error(new TransientTelegramException("5xx from Telegram"));
                    }
                    return resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(new PermanentTelegramException(
                                    "Telegram returned %s: %s".formatted(resp.statusCode(), body))));
                })
                .retryWhen(telegramClient.telegramRetry()).then();
    }
}
