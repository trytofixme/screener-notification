package ru.screener.notification.integrations.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.screener.notification.config.telegram.GlobalRateLimiter;
import ru.screener.notification.dto.telegram.TelegramSendRequestDto;
import ru.screener.notification.errors.telegram.PermanentTelegramException;
import ru.screener.notification.errors.telegram.RetryAfterException;
import ru.screener.notification.errors.telegram.TooManyRequestsException;
import ru.screener.notification.errors.telegram.TransientTelegramException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClient {

    private static final String SEND_MESSAGE_METHOD = "/sendMessage";
    private final WebClient webClient;
    private final GlobalRateLimiter globalLimiter;

    public Mono<Void> send(TelegramSendRequestDto dto) {
        return globalLimiter.acquire()
                .then(webClient.post()
                        .uri(SEND_MESSAGE_METHOD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto)
                        .exchangeToMono(resp -> {
                            if (resp.statusCode().is2xxSuccessful()) return resp.releaseBody();

                            if (resp.statusCode().value() == 429) {
                                return resp.bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .flatMap(body -> {
                                            Integer secs = null;
                                            // 1) заголовок
                                            List<String> ra = resp.headers().asHttpHeaders().get("Retry-After");
                                            if (ra != null && !ra.isEmpty()) {
                                                try { secs = Integer.parseInt(ra.get(0)); } catch (NumberFormatException ignore) {}
                                            }
                                            // 2) JSON parameters.retry_after
                                            if (secs == null && !body.isEmpty()) {
                                                try {
                                                    var node = new ObjectMapper().readTree(body);
                                                    var p = node.path("parameters").path("retry_after");
                                                    if (p.isInt()) secs = p.asInt();
                                                } catch (Exception ignore) {}
                                            }
                                            if (secs != null) return Mono.error(new RetryAfterException(secs));
                                            return Mono.error(new TooManyRequestsException());
                                        });
                            }

                            if (resp.statusCode().is5xxServerError()) {
                                return resp.releaseBody().then(Mono.error(new TransientTelegramException("5xx from Telegram")));
                            }
                            return resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new PermanentTelegramException(
                                            "Telegram returned %s: %s".formatted(resp.statusCode(), body))));
                        })
                )
                .onErrorMap(this::mapIoLikeToTransient);
    }

    public Retry telegramRetry() {
        final long SMALL_RETRY_CUTOFF_SEC = 5;  // всё, что >5с — уходит в DLT на отложку
        return Retry.from(companion -> companion.handle((signal, sink) -> {
            Throwable e = signal.failure();
            if (e == null) { sink.next(Duration.ZERO); return; }

            if (e instanceof RetryAfterException rae) {
                if (rae.seconds() <= SMALL_RETRY_CUTOFF_SEC) {
                    sink.next(Duration.ofSeconds(rae.seconds()));
                } else {
                    sink.error(e); // пусть выше решают про DLT
                }
                return;
            }
            if (e instanceof TooManyRequestsException) {
                long n = signal.totalRetries();
                if (n >= 3) { sink.error(e); return; }
                sink.next(jitterBackoff(n, 500, 4000));
                return;
            }
            if (e instanceof TransientTelegramException) {
                long n = signal.totalRetries();
                if (n >= 5) { sink.error(e); return; }
                sink.next(jitterBackoff(n, 300, 5000));
                return;
            }
            sink.error(e);
        }));
    }

    private Throwable mapIoLikeToTransient(Throwable e) {
        if (e instanceof TransientTelegramException) return e;
        if (e instanceof java.util.concurrent.TimeoutException) return new TransientTelegramException("Timeout");
        if (e instanceof reactor.netty.http.client.PrematureCloseException) return new TransientTelegramException("Premature close");
        if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException) return new TransientTelegramException("IO");
        if (e instanceof io.netty.channel.ConnectTimeoutException) return new TransientTelegramException("Connect timeout");
        if (e instanceof io.netty.handler.timeout.ReadTimeoutException) return new TransientTelegramException("Read timeout");
        return e;
    }

    private static Duration jitterBackoff(long attempt, long baseMs, long maxMs) {
        double exp = Math.min(maxMs, baseMs * Math.pow(2, attempt));
        double jitter = 0.1 + Math.random() * 0.2;
        return Duration.ofMillis((long) (exp * (1.0 + jitter)));
    }
}
