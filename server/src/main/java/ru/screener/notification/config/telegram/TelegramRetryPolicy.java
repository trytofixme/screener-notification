package ru.screener.notification.config.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;
import ru.screener.notification.errors.telegram.RetryAfterException;
import ru.screener.notification.errors.telegram.TooManyRequestsException;
import ru.screener.notification.errors.telegram.TransientTelegramException;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class TelegramRetryPolicy {

    private final TelegramProperties telegramProperties;

    public Retry retry() {
        var retryProps = telegramProperties.getRetry();
        return Retry.from(companion -> companion.handle((signal, sink) -> {
            Throwable ex = signal.failure();
            if (ex == null) {
                sink.next(Duration.ZERO);
                return;
            }

            if (ex instanceof RetryAfterException rae) {
                sink.next(Duration.ofSeconds(rae.seconds()));
                return;
            }

            long totalRetries = signal.totalRetries();
            if (ex instanceof TooManyRequestsException) {
                if (totalRetries >= retryProps.getTooManyRequestsMax()) {
                    sink.error(ex);
                    return;
                }

                sink.next(jitter(retryProps.getBase429Ms(), totalRetries, retryProps.getMaxMs(), retryProps));
                return;
            }

            if (ex instanceof TransientTelegramException
                    || ex instanceof WebClientRequestException
                    || ex instanceof java.io.IOException
                    || ex instanceof io.netty.handler.timeout.ReadTimeoutException) {
                if (totalRetries >= retryProps.getTransientMax()) {
                    sink.error(ex); return;
                }

                sink.next(jitter(retryProps.getBaseMs(), totalRetries, retryProps.getMaxMs(), retryProps));
                return;
            }
            sink.error(ex);
        }));
    }

    private static Duration jitter(int baseMs, long attempt, int maxMs, TelegramProperties.RetryProps retryProps) {
        double exp = Math.min(maxMs, baseMs * Math.pow(2, attempt));
        double jitPower = retryProps.getJitterLow() + ThreadLocalRandom.current().nextDouble(retryProps.getJitterHigh() - retryProps.getJitterLow());
        return Duration.ofMillis((long)(exp * (1.0 + jitPower)));
    }
}
