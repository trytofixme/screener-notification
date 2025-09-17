package ru.screener.notification.integrations.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.screener.notification.config.telegram.TelegramProperties;
import ru.screener.notification.dto.telegram.TelegramSendRequestDto;
import ru.screener.notification.errors.telegram.RetryAfterException;
import ru.screener.notification.errors.telegram.TooManyRequestsException;
import ru.screener.notification.errors.telegram.TransientTelegramException;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClient {

    private static final String SEND_MESSAGE_METHOD = "/sendMessage";

    private final WebClient webClient;

    public Mono<ClientResponse> doPost(TelegramSendRequestDto dto) {
        return webClient.post()
                .uri(SEND_MESSAGE_METHOD)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchangeToMono(Mono::just);
    }

    public Retry telegramRetry() {
        return Retry.from(companion -> companion.handle((signal, sink) -> {
            Throwable e = signal.failure();
            switch (e) {
                case null -> {
                    sink.next(Duration.ZERO);
                    return;
                }
                case RetryAfterException rae -> {
                    sink.next(Duration.ofSeconds(rae.seconds()));
                    return;
                }
                case TooManyRequestsException ignored -> {
                    long n = signal.totalRetries();
                    if (n >= 3) {
                        sink.error(e);
                        return;
                    }

                    Duration backoff = jitterBackoff(n, 500, 4000);
                    sink.next(backoff);
                    return;
                }
                default -> {
                }
            }

            if (e instanceof TransientTelegramException ||
                    is5xxOrIo(signal)) {
                long n = signal.totalRetries();
                if (n >= 5) { sink.error(e); return; }
                Duration backoff = jitterBackoff(n, 300, 5000);
                sink.next(backoff);
                return;
            }

            sink.error(e);
        }));
    }

    private boolean is5xxOrIo(Retry.RetrySignal signal) {
        Throwable f = signal.failure();
        return f instanceof TransientTelegramException
                || f instanceof WebClientRequestException
                || f instanceof io.netty.handler.timeout.ReadTimeoutException
                || f instanceof java.io.IOException;
    }

    private static Duration jitterBackoff(long attempt, long baseMs, long maxMs) {
        double exp = Math.min(maxMs, baseMs * Math.pow(2, attempt));
        double jitter = 0.1 + Math.random() * 0.2;
        return Duration.ofMillis((long) (exp * (1.0 + jitter)));
    }
}
